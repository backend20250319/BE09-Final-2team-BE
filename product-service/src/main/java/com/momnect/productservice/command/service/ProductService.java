package com.momnect.productservice.command.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NumberRangeQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.momnect.productservice.command.client.FileClient;
import com.momnect.productservice.command.client.ReviewClient;
import com.momnect.productservice.command.client.UserClient;
import com.momnect.productservice.command.client.dto.ChildDTO;
import com.momnect.productservice.command.client.dto.ImageFileDTO;
import com.momnect.productservice.command.client.dto.ReviewCountDTO;
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
import org.springframework.lang.Nullable;
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
    private final ReviewClient reviewClient;

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

    /**
     * 찜 추가
     */
    public void addWishlist(Long productId, Long userId) {
        // 이미 존재하는 경우 중복 저장 방지
        wishlistRepository.findByProductIdAndUserId(productId, userId)
                .ifPresent(w -> {
                    throw new IllegalStateException("이미 찜한 상품입니다.");
                });

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. id=" + productId));

        Wishlist wishlist = Wishlist.builder()
                .product(product)
                .userId(userId)
                .build();

        wishlistRepository.save(wishlist);
    }

    /**
     * 찜 취소
     */
    public void removeWishlist(Long productId, Long userId) {
        Wishlist wishlist = wishlistRepository.findByProductIdAndUserId(productId, userId)
                .orElseThrow(() -> new IllegalArgumentException("찜한 내역이 없습니다."));
        wishlistRepository.delete(wishlist);
    }

    /**
     * 내 찜 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ProductSummaryDto> getMyWishlist(Long userId) {
        // 유저가 찜한 상품 목록 조회
        List<Wishlist> wishlists = wishlistRepository.findAllByUserId(userId);

        // Wishlist → Product 추출
        List<Product> products = wishlists.stream()
                .map(Wishlist::getProduct)
                .toList();

        // 이미 로그인 유저 id가 있으므로 그대로 넘겨주면 inWishlist = true 처리됨
        return toProductSummaryDtos(products, userId);
    }

    /**
     * 유사 상품 조회
     */
    public List<ProductSummaryDto> getSimilarProducts(String keyword, Long userId) throws IOException {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("검색 키워드는 필수입니다.");
        }

        // Elasticsearch BoolQuery
        BoolQuery.Builder boolQuery = new BoolQuery.Builder()
                .must(m -> m.term(t -> t.field("isDeleted").value(false)))
                .mustNot(m -> m.term(t -> t.field("tradeStatus").value("SOLD")));

        // 키워드 검색
        boolQuery.should(s -> s.match(m -> m.field("name").query(keyword)))
                .should(s -> s.match(m -> m.field("content").query(keyword)))
                .should(s -> s.match(m -> m.field("hashtags").query(keyword)))
                .minimumShouldMatch("1");

        // 실행
        SearchResponse<ProductDocument> response = esClient.search(s -> s
                        .index("products")
                        .size(30)
                        .query(q -> q.bool(boolQuery.build()))
                        .sort(o -> o.field(f -> f.field("createdAt").order(SortOrder.Desc))),
                ProductDocument.class);

        // 현재 유저가 찜한 상품들
        Set<Long> wishlistIds = (userId != null)
                ? wishlistRepository.findProductIdsByUserId(userId)
                : Collections.emptySet();

        // 변환
        return response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(doc -> {
                    ProductSummaryDto dto = ProductSummaryDto.fromDocument(doc);

                    dto.setInWishlist(wishlistIds.contains(dto.getId()));

                    // 썸네일 절대경로 보정
                    if (doc.getThumbnailImagePath() != null) {
                        dto.setThumbnailUrl(toAbsoluteUrl(doc.getThumbnailImagePath()));
                    }

                    return dto;
                })
                .toList();
    }

    /**
     * 홈 상품 조회
     * - 인기 상품
     * - 추천 상품
     * - 신규 상품
     */
    public ProductSectionsResponse getHomeProductSections(Long userId) {
        List<ProductSummaryDto> popular = getPopularTop30(userId);
        List<ProductSummaryDto> latest = getNewTop30(userId);
        List<ProductSummaryDto> recommended = getRecommendedTop30(userId);


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
        return toProductSummaryDtos(products, userId);

    }

    /**
     * 신규상품: createdAt DESC
     */
    public List<ProductSummaryDto> getNewTop30(Long userId) {
        List<Product> products =
                productRepository.findTop30ByIsDeletedFalseAndTradeStatusNotOrderByCreatedAtDesc(TradeStatus.SOLD);
        return toProductSummaryDtos(products, userId);
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

            return toProductSummaryDtos(top30, userId);
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
            return toProductSummaryDtos(likeRanked, userId);
        }

        // fallback → 인기 Top30
        return getPopularTop30(userId);
    }

    public Page<ProductSummaryDto> searchProducts(ProductSearchRequest request, Long userId) throws IOException {
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;

        // 필수값 검증 (query 또는 categoryId는 반드시 하나 필요)
        if ((request.getQuery() == null || request.getQuery().isBlank())
                && request.getCategoryId() == null) {
            throw new IllegalArgumentException("검색 조건(query 또는 categoryId) 중 하나는 필수입니다.");
        }

        // BoolQuery 시작
        BoolQuery.Builder boolQuery = new BoolQuery.Builder()
                .must(m -> m.term(t -> t.field("isDeleted").value(false)));

        // 판매완료 제외 옵션
        if (Boolean.TRUE.equals(request.getExcludeSoldOut())) {
            boolQuery.mustNot(m -> m.term(t -> t.field("tradeStatus").value("SOLD")));
        }

        // 키워드 검색 (name, content, hashtags)
        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            boolQuery.should(s -> s.match(m -> m.field("name").query(request.getQuery())))
                    .should(s -> s.match(m -> m.field("content").query(request.getQuery())))
                    .should(s -> s.match(m -> m.field("hashtags").query(request.getQuery())))
                    .minimumShouldMatch("1");
        }

        // 카테고리
        if (request.getCategoryId() != null) {
            boolQuery.must(m -> m.term(t -> t.field("categoryId").value(request.getCategoryId())));
        }

        // 가격 범위 (NumberRangeQuery 사용)
        if (request.getPriceMin() != null || request.getPriceMax() != null) {
            NumberRangeQuery.Builder priceRange = new NumberRangeQuery.Builder().field("price");

            if (request.getPriceMin() != null) {
                priceRange.gte(request.getPriceMin().doubleValue());
            }
            if (request.getPriceMax() != null) {
                priceRange.lte(request.getPriceMax().doubleValue());
            }

            boolQuery.must(m -> m.range(r -> r.number(priceRange.build())));
        }

        // 추천 연령대 (enum → name)
        if (request.getAgeGroups() != null && !request.getAgeGroups().isEmpty()) {
            boolQuery.must(m -> m.terms(t -> t.field("recommendedAge.keyword")
                    .terms(ts -> ts.value(request.getAgeGroups().stream()
                            .map(Enum::name)
                            .map(FieldValue::of)
                            .toList()))));
        }

        // 지역
        if (request.getAreaIds() != null && !request.getAreaIds().isEmpty()) {
            boolQuery.must(m -> m.terms(t -> t.field("tradeAreaIds")
                    .terms(ts -> ts.value(request.getAreaIds().stream()
                            .map(FieldValue::of)
                            .toList()))));
        }

        // 상태 (NEW, USED)
        if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
            boolQuery.must(m -> m.terms(t -> t.field("productStatus.keyword")
                    .terms(ts -> ts.value(request.getStatuses().stream()
                            .map(Enum::name)
                            .map(v -> FieldValue.of(JsonData.of(v)))
                            .toList()))));
        }

        // 정렬 옵션 매핑
        SortOptions sortOption;
        switch (request.getSort()) {
            case LATEST -> sortOption = new SortOptions.Builder()
                    .field(f -> f.field("createdAt").order(SortOrder.Desc)).build();
            case PRICE_ASC -> sortOption = new SortOptions.Builder()
                    .field(f -> f.field("price").order(SortOrder.Asc)).build();
            case PRICE_DESC -> sortOption = new SortOptions.Builder()
                    .field(f -> f.field("price").order(SortOrder.Desc)).build();
            default -> sortOption = new SortOptions.Builder()
                    .field(f -> f.field("createdAt").order(SortOrder.Desc)).build();
        }

        // 검색 실행
        SearchResponse<ProductDocument> response = esClient.search(s -> s
                        .index("products")
                        .from(page * size)
                        .size(size)
                        .query(q -> q.bool(boolQuery.build()))
                        .sort(sortOption),
                ProductDocument.class);

        Set<Long> wishlistProductIds;
        if (userId != null) {
            wishlistProductIds = wishlistRepository.findProductIdsByUserId(userId);
        } else {
            wishlistProductIds = Collections.emptySet();
        }

        List<ProductSummaryDto> contents = response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(doc -> {
                    ProductSummaryDto dto = ProductSummaryDto.fromDocument(doc);

                    // 찜 여부
                    dto.setInWishlist(wishlistProductIds.contains(dto.getId()));

                    // 썸네일 URL
                    if (doc.getThumbnailImagePath() != null) {
                        // 절대 경로가 아니라면 도메인 붙여주기
                        String thumbnailUrl = toAbsoluteUrl(doc.getThumbnailImagePath());
                        dto.setThumbnailUrl(thumbnailUrl);
                    }

                    return dto;
                })
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

        return toProductSummaryDtos(products, userId);

//        // 상품 이미지 매핑 : productId -> 첫 번째 이미지 ID
//        Map<Long, Long> productImageIdMap = products.stream()
//                .collect(Collectors.toMap(
//                        Product::getId,
//                        p -> p.getProductImages().stream()
//                                .min(Comparator.comparingInt(ProductImage::getSortOrder))
//                                .orElseThrow(() -> new IllegalStateException("Product has no images"))
//                                .getId()
//                                .getImageFileId()
//                ));
//
//        // FileService 요청
//        String idsParam = productImageIdMap.values().stream()
//                .map(String::valueOf)
//                .collect(Collectors.joining(","));
//
//        Map<Long, String> imageUrlMap = new HashMap<>();
//        ApiResponse<List<ImageFileDTO>> response = fileClient.getImageFilesByIds(idsParam);
//        for (ImageFileDTO dto : response.getData()) {
//            imageUrlMap.put(dto.getId(), dto.getPath());
//        }
//
//        // 찜 여부 조회 (로그인 시만)
//        Map<Long, Boolean> likedMap;
//        if (userId != null) {
//            List<Wishlist> wishlists = wishlistRepository.findAllByUserIdAndProductIdIn(userId, productIds);
//            likedMap = wishlists.stream()
//                    .collect(Collectors.toMap(
//                            w -> w.getProduct().getId(),
//                            w -> true
//                    ));
//        } else {
//            likedMap = Collections.emptyMap();
//        }
//
//        // DTO 변환
//        return products.stream().map(product -> {
//            Long imageId = productImageIdMap.get(product.getId());
//            String thumbnailUrl = toAbsoluteUrl(imageUrlMap.get(imageId));
//
//            Boolean isLiked = likedMap.getOrDefault(product.getId(), false);
//
//            // 읍면동
//            String emd = product.getTradeAreas().get(0).getArea().getName();
//
//            return ProductSummaryDto.builder()
//                    .id(product.getId())
//                    .sellerId(product.getSellerId())
//                    .name(product.getName())
//                    .thumbnailUrl(thumbnailUrl)
//                    .inWishlist(isLiked)
//                    .price(product.getPrice())
//                    .emd(emd)
//                    .createdAt(product.getCreatedAt())
//                    .productStatus(product.getProductStatus().name())
//                    .tradeStatus(product.getTradeStatus().name())
//                    .isDeleted(product.getIsDeleted())
//                    .build();
//        }).toList();
    }


    /***
     * 상품 상세 조회
     * @param productId 조회할 상품 ID
     * @return 상품 엔티티
     */
    @Transactional(readOnly = true)
    public ProductDetailDTO getProductDetail(Long productId, Long userId) {
        // 1. 상품 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + productId));

        // 2. 판매자 정보 조회 + 거래/리뷰 데이터 보강
        Long sellerId = product.getSellerId();
        UserDTO sellerInfo = getSellerInfo(sellerId);

        // 3. 판매자의 최신 상품 3개 DTO 변환
        List<Product> sellerProducts = productRepository.findTop3BySellerIdOrderByCreatedAtDesc(sellerId);
        List<ProductSummaryDto> latestProductDtos = toProductSummaryDtos(sellerProducts, userId);

        // 4. 현재 상품 이미지 전체 DTO 변환
        List<ProductImageDTO> images = toProductImageDtos(product);

        // 5. 찜 여부 및 상품 찜 수
        boolean inWishlist = false;
        if (userId != null) {
            inWishlist = wishlistRepository.existsByUserIdAndProductId(userId, productId);
        }
        long wishlistCount = wishlistRepository.countByProductId(productId);

        // 6. 최종 DTO 반환
        return ProductDetailDTO.builder()
                .currentProduct(ProductDTO.fromEntity(product, images, (int) wishlistCount, inWishlist))
                .sellerInfo(sellerInfo)
                .sellerRecentProducts(latestProductDtos)
                .build();
    }


    // 판매자 정보 조회
    private UserDTO getSellerInfo(Long sellerId) {
        ApiResponse<UserDTO> sellerResponse = userClient.getUserInfo(sellerId);
        UserDTO sellerInfo = sellerResponse.getData();

        Integer tradeCount =
                productRepository.countByTradeStatusAndSellerIdOrBuyerId(TradeStatus.SOLD, sellerId, sellerId);
        // TODO: 리뷰 API 연동
        Integer reviewCount = 0;
        try {
            ReviewCountDTO resp = reviewClient.getReceivedReviewCount(sellerId);
            reviewCount = (resp != null) ? resp.getCount() : 0;
        } catch (Exception e) {
            // 실패 시 안전하게 0으로 fallback
            reviewCount = 0;
        }

        sellerInfo.setTradeCount(tradeCount);
        sellerInfo.setReviewCount(reviewCount);

        return sellerInfo;
    }

    // 상품 요약 정보 리스트로 변환
    public List<ProductSummaryDto> toProductSummaryDtos(
            List<Product> products,
            @Nullable Long loginUserId
    ) {
        if (products.isEmpty()) return List.of();

        // 썸네일 추출
        Map<Long, Long> productToThumbnailId = products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> p.getProductImages().stream()
                                .min(Comparator.comparingInt(ProductImage::getSortOrder))
                                .orElseThrow(() -> new IllegalStateException("상품 이미지 없음: " + p.getId()))
                                .getId()
                                .getImageFileId()
                ));

        // FileClient 조회
        Map<Long, String> paths = resolveImagePaths(productToThumbnailId.values());

        // 로그인한 경우 → 유저의 위시리스트 ID 한 번만 조회
        Set<Long> wishlistIds = loginUserId != null
                ? wishlistRepository.findProductIdsByUserId(loginUserId)
                : Set.of();

        return products.stream()
                .map(p -> ProductSummaryDto.fromEntity(
                        p,
                        toAbsoluteUrl(paths.get(productToThumbnailId.get(p.getId()))),
                        wishlistIds.contains(p.getId())
                ))
                .toList();
    }


    // 상품 이미지 리스트
    private List<ProductImageDTO> toProductImageDtos(Product product) {
        List<Long> imageIds = product.getProductImages().stream()
                .map(img -> img.getId().getImageFileId())
                .toList();

        Map<Long, String> paths = resolveImagePaths(imageIds);

        return product.getProductImages().stream()
                .sorted(Comparator.comparingInt(ProductImage::getSortOrder))
                .map(img -> ProductImageDTO.builder()
                        .imageFileId(img.getId().getImageFileId())
                        .sortOrder(img.getSortOrder())
                        .url(toAbsoluteUrl(paths.get(img.getId().getImageFileId())))
                        .build()
                )
                .toList();
    }

    // 이미지 경로 가져오기
    private Map<Long, String> resolveImagePaths(Collection<Long> imageIds) {
        if (imageIds.isEmpty()) return Map.of();

        String idsParam = imageIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        ApiResponse<List<ImageFileDTO>> response = fileClient.getImageFilesByIds(idsParam);

        return response.getData().stream()
                .collect(Collectors.toMap(ImageFileDTO::getId, ImageFileDTO::getPath));
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

        /** Elasticsearch 색인 **/
        // emd 추출
        Area area = areaRepository.findById(dto.getAreaIds().get(0))
                .orElseThrow(() -> new IllegalArgumentException("Invalid area ID: " + dto.getAreaIds().get(0)));
        String emd = area.getName();


        // 썸네일 추출
        Long firstImageFileId = dto.getImageFileIds().get(0);
        ApiResponse<List<ImageFileDTO>> response = fileClient.getImageFilesByIds(firstImageFileId.toString());
        String thumbnailImagePath = response.getData().get(0).getPath();

        indexProduct(saved, emd, thumbnailImagePath, dto.getAreaIds());

        return saved.getId();
    }
    // ------------ util --------------

    /***
     * Elasticsearch에 상품 데이터를 색인
     * @param product 색인할 상품 엔티티
     * @param emd
     * @param thumbnailImagePath
     */
    public void indexProduct(Product product, String emd, String thumbnailImagePath, List<Integer> tradeAreaIds) throws IOException {
        ProductDocument doc = ProductDocument.fromEntity(product, emd, thumbnailImagePath, tradeAreaIds);

        esClient.index(i -> i
                .index("products")
                .id(doc.getId().toString())
                .document(doc)
        );
    }


    /**
     * FileClient를 통해 이미지 파일 ID → 절대 URL 맵핑을 조회한다.
     *
     * @param fileIds 조회할 파일 ID 리스트
     * @return fileId → 절대 URL 매핑
     */
    private Map<Long, String> getFileUrlsByIds(Collection<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) return Collections.emptyMap();

        String idsParam = fileIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        ApiResponse<List<ImageFileDTO>> resp = fileClient.getImageFilesByIds(idsParam);

        if (resp == null || resp.getData() == null) {
            return Collections.emptyMap();
        }

        return resp.getData().stream()
                .collect(Collectors.toMap(
                        ImageFileDTO::getId,
                        dto -> toAbsoluteUrl(dto.getPath())
                ));
    }
}
