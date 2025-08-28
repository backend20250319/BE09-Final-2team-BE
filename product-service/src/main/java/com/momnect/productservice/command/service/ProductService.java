package com.momnect.productservice.command.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NumberRangeQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.momnect.productservice.command.client.FileClient;
import com.momnect.productservice.command.client.UserClient;
import com.momnect.productservice.command.client.dto.ChildDTO;
import com.momnect.productservice.command.client.dto.ImageFileDTO;
import com.momnect.productservice.command.client.dto.UserDTO;
import com.momnect.productservice.command.document.ProductDocument;
import com.momnect.productservice.command.dto.image.ProductImageDTO;
import com.momnect.productservice.command.dto.product.*;
import com.momnect.productservice.command.entity.area.Area;
import com.momnect.productservice.command.entity.area.ProductTradeArea;
import com.momnect.productservice.command.entity.area.ProductTradeAreaId;
import com.momnect.productservice.command.entity.hashtag.Hashtag;
import com.momnect.productservice.command.entity.hashtag.ProductHashtag;
import com.momnect.productservice.command.entity.hashtag.ProductHashtagId;
import com.momnect.productservice.command.entity.image.ProductImage;
import com.momnect.productservice.command.entity.image.ProductImageId;
import com.momnect.productservice.command.entity.product.*;
import com.momnect.productservice.command.repository.*;
import com.momnect.productservice.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
    private final AreaRepository areaRepository;
    private final ProductTradeAreaRepository productTradeAreaRepository;
    private final HashtagRepository hashtagRepository;
    private final WishlistRepository wishlistRepository;

    private final ElasticsearchClient esClient;


    @Value("${ftp.base-url}")
    private String ftpBaseUrl;

    private String toAbsoluteUrl(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) return null;
        return ftpBaseUrl + relativePath;
    }

    // ====================== 홈 섹션 ======================
    public ProductSectionsResponse getHomeProductSections(Long userId) {
        List<ProductSummaryDto> popular = getPopularTop30(userId);
        List<ProductSummaryDto> latest = getNewTop30(userId);
        List<ProductSummaryDto> recommended = getRecommendedTop30(userId);

//        List<ProductSummaryDto> popular = new ArrayList<>();
//        List<ProductSummaryDto> latest = new ArrayList<>();
//        List<ProductSummaryDto> recommended = new ArrayList<>();

        return ProductSectionsResponse.builder()
                .popular(popular)
                .latest(latest)
                .recommended(recommended)
                .build();
    }

    /**
     * 인기상품: viewCount DESC, createdAt DESC
     */
    public List<ProductSummaryDto> getPopularTop30(Long userId) {
        List<Product> products =
                productRepository.findTop30ByIsDeletedFalseAndTradeStatusNotOrderByViewCountDescCreatedAtDesc(TradeStatus.SOLD);
        return toSummaries(products, userId);
    }

    /**
     * 신규상품: createdAt DESC
     */
    public List<ProductSummaryDto> getNewTop30(Long userId) {
        List<Product> products =
                productRepository.findTop30ByIsDeletedFalseAndTradeStatusNotOrderByCreatedAtDesc(TradeStatus.SOLD);
        return toSummaries(products, userId);
    }

    /**
     * 추천상품
     * - userId가 있으면 자녀 연령대(복수)를 계산해 해당 버킷의 상품을 모아 상위 30개 반환
     * - 없거나 자녀정보가 없으면 기존 "찜수 TOP N → 인기 Top30" 로직 유지
     */
    public List<ProductSummaryDto> getRecommendedTop30(Long userId) {
        // 1) userId 있으면 자녀정보로 연령대 버킷 수집 (별도 함수 없이 이 메서드 안에서 처리)
        Set<RecommendedAge> ageBuckets = new HashSet<>();
        if (userId != null) {
            try {
                ApiResponse<List<ChildDTO>> resp = userClient.getChildren();

                List<ChildDTO> children =
                        Optional.ofNullable(resp)
                                .map(com.momnect.productservice.common.ApiResponse::getData)
                                .orElse(java.util.Collections.emptyList());

                System.out.println("children: " + children);

                LocalDate today = java.time.LocalDate.now();
                for (var child : children) {
                    LocalDate birthDate = child.getBirthDate();
                    long months = ChronoUnit.MONTHS.between(birthDate, today);
                    long years = ChronoUnit.YEARS.between(birthDate, today);
                    if (months >= 0) {
                        if (months < 6) ageBuckets.add(RecommendedAge.MONTH_0_6);
                        else if (months < 12) ageBuckets.add(RecommendedAge.MONTH_6_12);
                        else if (years < 2) ageBuckets.add(RecommendedAge.YEAR_1_2);
                        else if (years < 4) ageBuckets.add(RecommendedAge.YEAR_2_4);
                        else if (years < 6) ageBuckets.add(RecommendedAge.YEAR_4_6);
                        else if (years < 8) ageBuckets.add(RecommendedAge.YEAR_6_8);
                        else ageBuckets.add(RecommendedAge.OVER_8);
                    }
                }
            } catch (Exception ignore) {
                // 유저서비스 실패 시 필터 없이 아래 랭킹 로직으로 폴백
            }
        }

        // ageBuckets가 비어있지 않은 경우
        if (!ageBuckets.isEmpty()) {
            System.out.println("자녀 추천 -- ageBuckets: " + ageBuckets);

            // IN 한 번에 조회 (DB에서 createdAt DESC → viewCount DESC 정렬까지 처리)
            List<Product> candidates =
                    productRepository.findTop100ByIsDeletedFalseAndTradeStatusNotAndRecommendedAgeInOrderByCreatedAtDescViewCountDesc(
                            TradeStatus.SOLD, ageBuckets
                    );

            // 안전 필터 + 상위 30개만
            List<Product> top30 = candidates.stream()
                    .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()) && p.getTradeStatus() != TradeStatus.SOLD)
                    .limit(30)
                    .toList();

            return toSummaries(top30, userId);
        }

        // 3) 기존 랭킹 로직 (찜수 TOP N → 인기 Top30)
        java.util.List<Long> topLikeIds = wishlistRepository.findTopProductIdsByLikeCount(org.springframework.data.domain.PageRequest.of(0, 30));
        if (!topLikeIds.isEmpty()) {
            java.util.List<Product> likeRanked = productRepository.findByIdIn(topLikeIds).stream()
                    .filter(p -> !java.lang.Boolean.TRUE.equals(p.getIsDeleted()) && p.getTradeStatus() != TradeStatus.SOLD)
                    .toList();

            java.util.Map<Long, Integer> order = new java.util.HashMap<>();
            for (int i = 0; i < topLikeIds.size(); i++) order.put(topLikeIds.get(i), i);

            likeRanked = new java.util.ArrayList<>(likeRanked);
            likeRanked.sort(java.util.Comparator.comparingInt(p -> order.getOrDefault(p.getId(), Integer.MAX_VALUE)));

            return toSummaries(likeRanked, userId);
        }

        // fallback → 인기 Top30
        return getPopularTop30(userId);
    }


    private List<ProductSummaryDto> toSummaries(List<Product> products, Long userId) {
        if (products == null || products.isEmpty()) return List.of();

        // (1) 유저 찜 여부 일괄 조회
        Set<Long> likedIds;
        if (userId != null) {
            List<Long> ids = products.stream().map(Product::getId).toList();
            likedIds = wishlistRepository.findAllByUserIdAndProductIdIn(userId, ids).stream()
                    .map(w -> w.getProduct().getId())
                    .collect(Collectors.toSet());
        } else {
            likedIds = Collections.emptySet();
        }

        // (2) 썸네일 URL 배치 로딩 (productId -> url)
        Map<Long, String> thumbUrlByProductId = fetchThumbnailUrlByProductId(products);

        // (3) DTO 변환
        return products.stream()
                .map(p -> ProductSummaryDto.fromEntity(
                        p,
                        thumbUrlByProductId.get(p.getId()),
                        userId != null && likedIds.contains(p.getId())
                ))
                .toList();
    }

    private Map<Long, String> fetchThumbnailUrlByProductId(List<Product> products) {
        // 1) productId -> 대표 이미지 fileId(최소 sortOrder) 추출
        Map<Long, Long> productToFileId = new HashMap<>();
        for (Product p : products) {
            if (p.getProductImages() == null || p.getProductImages().isEmpty()) continue;

            p.getProductImages().stream()
                    .min(Comparator.comparingInt(ProductImage::getSortOrder))
                    .ifPresent(img -> productToFileId.put(p.getId(), img.getId().getImageFileId()));
        }
        if (productToFileId.isEmpty()) return Collections.emptyMap();

        // 2) FileClient 배치 호출
        String idsParam = productToFileId.values().stream()
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        ApiResponse<List<ImageFileDTO>> resp = fileClient.getImageFilesByIds(idsParam);

        // 3) imageFileId -> 절대경로 URL 매핑
        Map<Long, String> fileIdToAbsUrl = new HashMap<>();
        if (resp != null && resp.getData() != null) {
            for (ImageFileDTO dto : resp.getData()) {
                fileIdToAbsUrl.put(dto.getId(), toAbsoluteUrl(dto.getPath()));
            }
        }

        // 4) productId -> 썸네일 URL 매핑으로 변환
        Map<Long, String> result = new HashMap<>();
        for (Map.Entry<Long, Long> e : productToFileId.entrySet()) {
            result.put(e.getKey(), fileIdToAbsUrl.get(e.getValue())); // 없으면 null
        }
        return result;
    }


    private <T> List<T> limit(List<T> list, int n) {
        return list.size() <= n ? list : list.subList(0, n);
    }

    public Page<ProductSummaryDto> searchProducts(ProductSearchRequest request) throws IOException {
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;

        // BoolQuery 빌드
        var boolQuery = new BoolQuery.Builder()
                .must(m -> m.term(t -> t.field("isDeleted").value(false)))
                .mustNot(m -> m.term(t -> t.field("tradeStatus").value("SOLD")));

        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            boolQuery.should(s -> s.match(m -> m.field("name").query(request.getQuery())))
                    .should(s -> s.match(m -> m.field("content").query(request.getQuery())))
                    .should(s -> s.match(m -> m.field("hashtags").query(request.getQuery())))
                    .minimumShouldMatch("1");
        }

        if (request.getCategoryId() != null) {
            boolQuery.must(m -> m.term(t -> t
                    .field("categoryId")
                    .value(request.getCategoryId().toString()))); // value는 보통 String 처리
        }

        if (request.getMinPrice() != null) {
            NumberRangeQuery minPriceQuery = new NumberRangeQuery.Builder()
                    .field("price")
                    .gte(request.getMinPrice().doubleValue())   // 그냥 Double/Long 값 넣기
                    .build();
            boolQuery.must(m -> m.range(r -> r.number(minPriceQuery)));
        }

        if (request.getMaxPrice() != null) {
            NumberRangeQuery maxPriceQuery = new NumberRangeQuery.Builder()
                    .field("price")
                    .lte(request.getMaxPrice().doubleValue())   // Integer → long
                    .build();
            boolQuery.must(m -> m.range(r -> r.number(maxPriceQuery)));
        }

        // 검색 실행
        SearchResponse<ProductDocument> response = esClient.search(s -> s
                        .index("products")
                        .from(page * size)
                        .size(size)
                        .query(q -> q.bool(boolQuery.build()))
                        .sort(sort -> sort.field(f -> f.field("createdAt").order(SortOrder.Desc))),
                ProductDocument.class);

        List<ProductSummaryDto> contents = response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(ProductSummaryDto::fromDocument)
                .toList();

        return new PageImpl<>(contents, PageRequest.of(page, size), response.hits().total().value());
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
                    .sellerId(product.getSellerId())
                    .name(product.getName())
                    .thumbnailUrl(thumbnailUrl)
                    .inWishlist(isLiked)
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
                            .inWishlist(false) // 필요시 세팅
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
    public Long createProduct(ProductRequest dto, String userId) throws IOException {
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
    public void indexProduct(Product product) throws IOException {
        ProductDocument doc = ProductDocument.fromEntity(product);

        esClient.index(i -> i
                .index("products")
                .id(doc.getId().toString())
                .document(doc)
        );
    }

}
