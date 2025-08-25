package com.momnect.productservice.command.service;

import com.momnect.productservice.command.client.FileClient;
import com.momnect.productservice.command.client.UserClient;
import com.momnect.productservice.command.client.dto.ImageFileDTO;
import com.momnect.productservice.command.client.dto.UserDTO;
import com.momnect.productservice.command.document.ProductDocument;
import com.momnect.productservice.command.dto.image.ProductImageDTO;
import com.momnect.productservice.command.dto.product.ProductDTO;
import com.momnect.productservice.command.dto.product.ProductDetailDTO;
import com.momnect.productservice.command.dto.product.ProductRequest;
import com.momnect.productservice.command.dto.product.ProductSummaryDto;
import com.momnect.productservice.command.entity.area.Area;
import com.momnect.productservice.command.entity.area.ProductTradeArea;
import com.momnect.productservice.command.entity.area.ProductTradeAreaId;
import com.momnect.productservice.command.entity.hashtag.Hashtag;
import com.momnect.productservice.command.entity.hashtag.ProductHashtag;
import com.momnect.productservice.command.entity.hashtag.ProductHashtagId;
import com.momnect.productservice.command.entity.image.ProductImage;
import com.momnect.productservice.command.entity.image.ProductImageId;
import com.momnect.productservice.command.entity.product.Product;
import com.momnect.productservice.command.entity.product.ProductCategory;
import com.momnect.productservice.command.entity.product.TradeStatus;
import com.momnect.productservice.command.entity.product.Wishlist;
import com.momnect.productservice.command.repository.*;
import com.momnect.productservice.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final FileClient fileClient;
    private final UserClient userClient;

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductSearchRepository searchRepository;
    private final AreaRepository areaRepository;
    private final ProductTradeAreaRepository productTradeAreaRepository;
    private final HashtagRepository hashtagRepository;
    private final WishlistRepository wishlistRepository;


    @Value("${ftp.base-url}")
    private String ftpBaseUrl;

    private String toAbsoluteUrl(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) return null;
        return ftpBaseUrl + relativePath;
    }

    /**
     * 주어진 상품 ID 목록에 해당하는 상품들의 요약 정보를 조회
     * - 각 상품의 첫 번째 이미지 URL을 조회(썸네일)
     * - 사용자가 로그인한 경우, 상품에 대한 찜 여부 확인 가능
     *
     * @param productIds 조회할 상품 ID 리스트
     * @param userId     요청한 사용자 ID (로그인하지 않은 경우 null 가능)
     * @return 상품 요약 정보를 담은 ProductSummaryDto 리스트
     */
    @Transactional(readOnly = true)
    public List<ProductSummaryDto> getProductsByIds(List<Long> productIds, Long userId) {
        // 상품 조회
        List<Product> products = productRepository.findAllById(productIds);

        // 상품 이미지 매핑 : productId -> 첫 번째 이미지 ID
        Map<Long, Long> productImageIdMap = products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> p.getProductImages().stream()
                                .min(Comparator.comparingInt(ProductImage::getSortOrder))
                                .orElseThrow(() -> new IllegalStateException("Product has no images"))
                                .getId()
                                .getImageFileId()
                ));

        // FileService 요청
        String idsParam = productImageIdMap.values().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        Map<Long, String> imageUrlMap = new HashMap<>();
        ApiResponse<List<ImageFileDTO>> response = fileClient.getImageFilesByIds(idsParam);
        for (ImageFileDTO dto : response.getData()) {
            imageUrlMap.put(dto.getId(), dto.getPath());
        }

        // 찜 여부 조회 (로그인 시만)
        Map<Long, Boolean> likedMap;
        if (userId != null) {
            List<Wishlist> wishlists = wishlistRepository.findAllByUserIdAndProductIdIn(userId, productIds);
            likedMap = wishlists.stream()
                    .collect(Collectors.toMap(
                            w -> w.getProduct().getId(),
                            w -> true
                    ));
        } else {
            likedMap = Collections.emptyMap();
        }

        // DTO 변환
        return products.stream().map(product -> {
            Long imageId = productImageIdMap.get(product.getId());
            String thumbnailUrl = toAbsoluteUrl(imageUrlMap.get(imageId));

            Boolean isLiked = likedMap.getOrDefault(product.getId(), false);

            // 읍면동
            String emd = product.getTradeAreas().get(0).getArea().getName();

            return ProductSummaryDto.builder()
                    .id(product.getId())
                    .thumbnailUrl(thumbnailUrl)
                    .isLiked(isLiked)
                    .price(product.getPrice())
                    .emd(emd)
                    .createdAt(product.getCreatedAt())
                    .productStatus(product.getProductStatus().name())
                    .tradeStatus(product.getTradeStatus().name())
                    .isDeleted(product.getIsDeleted())
                    .build();
        }).toList();
    }


    /***
     * 상품 상세 조회
     * @param productId 조회할 상품 ID
     * @return 상품 엔티티
     */
    @Transactional(readOnly = true)
    public ProductDetailDTO getProductDetail(Long productId) {
        // 엔티티 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + productId));

        // -- 판매 유저 조회 및 판매 유저의 최신 판매 상품 3개 조회 --
        // 판매자 정보 조회 (UserClient)
        Long sellerId = product.getSellerId();
        ApiResponse<UserDTO> sellerResponse = userClient.getUserInfo(sellerId);
        UserDTO sellerInfo = sellerResponse.getData();

        // 판매자 거래횟수 조회
        Integer tradeCount = productRepository.countByTradeStatusAndSellerIdOrBuyerId(TradeStatus.SOLD, sellerId, sellerId);
        // 판매자 리뷰개수 조회
        Integer reviewCount = 44;

        sellerInfo.setTradeCount(tradeCount);
        sellerInfo.setReviewCount(reviewCount);

        // 판매자의 최신 상품 3개 조회
        List<Product> sellerProducts = productRepository.findTop3BySellerIdOrderByCreatedAtDesc(sellerId);

        // sellerRecentProducts 리스트에서 각 상품의 대표 이미지(썸네일) ID를 추출하여 Map으로 변환
        // Map의 Key: 상품 ID, Value: 대표 이미지 ID (sortOrder가 가장 작은 이미지)
        Map<Long, Long> productToThumbnailId = sellerProducts.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        sellerProduct -> sellerProduct.getProductImages().stream()
                                .min(Comparator.comparingInt(ProductImage::getSortOrder))
                                .orElseThrow(() -> new IllegalStateException(
                                        "상품에 이미지가 존재하지 않습니다. 상품ID: " + sellerProduct.getId()))
                                .getId()
                                .getImageFileId()
                ));

        String sellerProductImageIdsParam = productToThumbnailId.values().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        ApiResponse<List<ImageFileDTO>> sellerProductResponse = fileClient.getImageFilesByIds(sellerProductImageIdsParam);

        // 3. ID → URL Map
        Map<Long, String> sellerProductPaths = sellerProductResponse.getData().stream()
                .collect(Collectors.toMap(ImageFileDTO::getId, ImageFileDTO::getPath));

        List<ProductSummaryDto> latestProductDtos = sellerProducts.stream()
                .map(sellerProduct -> {
                    Long thumbnailId = productToThumbnailId.get(sellerProduct.getId());
                    String thumbnailUrl = sellerProductPaths.get(thumbnailId);

                    return ProductSummaryDto.builder()
                            .id(sellerProduct.getId())
                            .thumbnailUrl(toAbsoluteUrl(thumbnailUrl))
                            .isLiked(false) // 필요시 세팅
                            .price(sellerProduct.getPrice())
                            .emd(sellerProduct.getTradeAreas().get(0).getArea().getName())
                            .createdAt(sellerProduct.getCreatedAt())
                            .productStatus(sellerProduct.getProductStatus().name())
                            .tradeStatus(sellerProduct.getTradeStatus().name())
                            .isDeleted(sellerProduct.getIsDeleted())
                            .build();
                })
                .toList();

        // 이미지 파일 ID 리스트 추출
        List<Long> imageIds = product.getProductImages()
                .stream()
                .map(img -> img.getId().getImageFileId())
                .toList();

        String idsParam = imageIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        ApiResponse<List<ImageFileDTO>> response = fileClient.getImageFilesByIds(idsParam);

        Map<Long, String> paths = response.getData().stream()
                .collect(Collectors.toMap(ImageFileDTO::getId, ImageFileDTO::getPath));

        List<ProductImageDTO> images = product.getProductImages().stream()
                .map(img -> ProductImageDTO.builder()
                        .imageFileId(img.getId().getImageFileId())
                        .sortOrder(img.getSortOrder())
                        .url(toAbsoluteUrl(paths.get(img.getId().getImageFileId())))
                        .build())
                .collect(Collectors.toList());


        return ProductDetailDTO.builder()
                .currentProduct(ProductDTO.fromEntity(product, images))
                .sellerInfo(sellerInfo)
                .sellerRecentProducts(latestProductDtos)
                .build();

    }

    /***
     * 상품 등록 기능
     * @param dto 상품 등록 요청 ProductRequest
     * @param userId
     * @return 등록된 상품의 ID
     */
    @Transactional
    public Long createProduct(ProductRequest dto, String userId) {
        ProductCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid category ID"));

        Product product = Product.fromRequest(dto, category, Long.valueOf(userId));
        Product saved = productRepository.save(product);

        // 상품 이미지 연결
        int sortOrder = 1;
        for (Long imageFileId : dto.getImageFileIds()) {
            ProductImageId id = new ProductImageId(saved.getId(), imageFileId);

            ProductImage image = ProductImage.builder()
                    .id(id)
                    .sortOrder(sortOrder++)
                    .product(saved)
                    .build();

            productImageRepository.save(image);
        }


        // 지역 연결
        for (Integer areaId : dto.getAreaIds()) {
            Area area = areaRepository.findById(areaId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid area ID: " + areaId));

            ProductTradeAreaId tradeAreaId = new ProductTradeAreaId(saved.getId(), area.getId());
            ProductTradeArea tradeArea = ProductTradeArea.builder()
                    .id(tradeAreaId)
                    .product(saved)
                    .area(area)
                    .build();

            productTradeAreaRepository.save(tradeArea);
        }

        // 해시태그 연결
        for (String tagName : dto.getHashtags()) {
            Hashtag hashtag = hashtagRepository.findByName(tagName)
                    .orElseGet(() -> hashtagRepository.save(Hashtag.builder().name(tagName).build()));

            ProductHashtagId phId = new ProductHashtagId(saved.getId(), hashtag.getId());
            ProductHashtag ph = ProductHashtag.builder()
                    .id(phId)
                    .product(saved)
                    .hashtag(hashtag)
                    .build();

            // hashtag list에 추가
            saved.getProductHashtags().add(ph);
        }

        // Elasticsearch 색인
        indexProduct(saved);

        return saved.getId();
    }

    /***
     * Elasticsearch에 상품 데이터를 색인
     * @param product 색인할 상품 엔티티
     */
    public void indexProduct(Product product) {
        ProductDocument doc = ProductDocument.fromEntity(product);
        searchRepository.save(doc);
    }
}
