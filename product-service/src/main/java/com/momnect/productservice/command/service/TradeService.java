package com.momnect.productservice.command.service;

import com.momnect.productservice.command.client.FileClient;
import com.momnect.productservice.command.client.dto.ImageFileDTO;
import com.momnect.productservice.command.dto.product.ProductSummaryDto;
import com.momnect.productservice.command.dto.trade.TradeSummaryDTO;
import com.momnect.productservice.command.entity.image.ProductImage;
import com.momnect.productservice.command.entity.product.Product;
import com.momnect.productservice.command.entity.product.TradeStatus;
import com.momnect.productservice.command.repository.ProductRepository;
import com.momnect.productservice.command.repository.WishlistRepository;
import com.momnect.productservice.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final FileClient fileClient;

    private final ProductRepository productRepository;
    private final WishlistRepository wishlistRepository;

    @Value("${ftp.base-url}")
    private String ftpBaseUrl;

    private String toAbsoluteUrl(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) return null;
        return ftpBaseUrl + relativePath;
    }

    /**
     * 내 거래 현황 요약 조회 (구매수/판매수)
     */
    @Transactional(readOnly = true)
    public TradeSummaryDTO getTradeSummary(Long userId, boolean isMyProfile) {
        // 총 판매상품 수 (판매 완료 여부 상관없이 판매자가 올린 모든 상품)
        Integer totalSalesCount = productRepository.countBySellerIdAndIsDeletedFalse(userId);

        // 판매 완료 상품 수
        Integer salesCount = productRepository.countByTradeStatusAndSellerId(
                TradeStatus.SOLD, userId);

        if (isMyProfile) {
            // 구매 완료 상품 수
            Integer purchaseCount = productRepository.countByTradeStatusAndBuyerId(TradeStatus.SOLD, userId);

            return TradeSummaryDTO.builder()
                    .totalSalesCount(totalSalesCount)     // 총 판매상품 수
                    .salesCount(salesCount)               // 판매 완료 상품 수
                    .purchaseCount(purchaseCount)         // 구매 완료 상품 수
                    .build();
        } else {
            // 타유저 거래 현황은 판매수만 리턴
            return TradeSummaryDTO.builder()
                    .totalSalesCount(totalSalesCount)     // 총 판매상품 수
                    .salesCount(salesCount)               // 판매 완료 상품 수
                    .build();
        }
    }

    /**
     * 내 구매 상품 조회
     */
    @Transactional(readOnly = true)
    public List<ProductSummaryDto> getMyPurchases(Long userId) {
        List<Product> products = productRepository.findByTradeStatusAndBuyerId(TradeStatus.SOLD, userId);

        if (products.isEmpty()) {
            return List.of();
        }

        // 2. 각 상품의 대표 이미지 ID 추출 (sortOrder가 가장 작은 이미지)
        Map<Long, Long> productToThumbnailId = products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        product -> product.getProductImages().stream()
                                .min(Comparator.comparingInt(ProductImage::getSortOrder))
                                .orElseThrow(() -> new IllegalStateException(
                                        "상품에 이미지가 존재하지 않습니다. 상품ID: " + product.getId()))
                                .getId()
                                .getImageFileId()
                ));

        // 3. file-service 호출해서 이미지 정보 조회
        String imageIdsParam = productToThumbnailId.values().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        ApiResponse<List<ImageFileDTO>> response = fileClient.getImageFilesByIds(imageIdsParam);

        // 4. ID → URL Map
        Map<Long, String> imageIdToUrl = response.getData().stream()
                .collect(Collectors.toMap(ImageFileDTO::getId, ImageFileDTO::getPath));

        // 5. ProductSummaryDto 변환
        return products.stream()
                .map(product -> {
                    Long thumbnailId = productToThumbnailId.get(product.getId());
                    String thumbnailUrl = imageIdToUrl.get(thumbnailId);

                    return ProductSummaryDto.builder()
                            .id(product.getId())
                            .thumbnailUrl(toAbsoluteUrl(thumbnailUrl))
                            .inWishlist(inWishlist(product.getId(), userId)) // 필요시 변경
                            .price(product.getPrice())
                            .emd(product.getTradeAreas().get(0).getArea().getName())
                            .createdAt(product.getCreatedAt())
                            .productStatus(product.getProductStatus().name())
                            .tradeStatus(product.getTradeStatus().name())
                            .isDeleted(product.getIsDeleted())
                            .build();
                })
                .toList();
    }

    /**
     * 내 판매 상품 조회
     */
    @Transactional(readOnly = true)
    public List<ProductSummaryDto> getMySales(Long userId) {
        List<Product> products = productRepository.findBySellerIdAndIsDeletedFalse(userId);

        if (products.isEmpty()) {
            return List.of();
        }

        // 2. 각 상품의 대표 이미지 ID 추출 (sortOrder가 가장 작은 이미지)
        Map<Long, Long> productToThumbnailId = products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        product -> product.getProductImages().stream()
                                .min(Comparator.comparingInt(ProductImage::getSortOrder))
                                .orElseThrow(() -> new IllegalStateException(
                                        "상품에 이미지가 존재하지 않습니다. 상품ID: " + product.getId()))
                                .getId()
                                .getImageFileId()
                ));

        // 3. file-service 호출해서 이미지 정보 조회
        String imageIdsParam = productToThumbnailId.values().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        ApiResponse<List<ImageFileDTO>> response = fileClient.getImageFilesByIds(imageIdsParam);

        // 4. ID → URL Map
        Map<Long, String> imageIdToUrl = response.getData().stream()
                .collect(Collectors.toMap(ImageFileDTO::getId, ImageFileDTO::getPath));

        // 5. ProductSummaryDto 변환
        return products.stream()
                .map(product -> {
                    Long thumbnailId = productToThumbnailId.get(product.getId());
                    String thumbnailUrl = imageIdToUrl.get(thumbnailId);

                    return ProductSummaryDto.builder()
                            .id(product.getId())
                            .thumbnailUrl(toAbsoluteUrl(thumbnailUrl))
                            .inWishlist(inWishlist(product.getId(), userId)) // 필요시 변경
                            .price(product.getPrice())
                            .emd(product.getTradeAreas().get(0).getArea().getName())
                            .createdAt(product.getCreatedAt())
                            .productStatus(product.getProductStatus().name())
                            .tradeStatus(product.getTradeStatus().name())
                            .isDeleted(product.getIsDeleted())
                            .build();
                })
                .toList();
    }

    /**
     * 특정 유저 판매 상품 조회
     */
    @Transactional(readOnly = true)
    public List<ProductSummaryDto> getUserSales(Long userId, Long sellerId) {
        List<Product> products = productRepository.findBySellerIdAndIsDeletedFalse(sellerId);

        if (products.isEmpty()) {
            return List.of();
        }

        // 2. 각 상품의 대표 이미지 ID 추출 (sortOrder가 가장 작은 이미지)
        Map<Long, Long> productToThumbnailId = products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        product -> product.getProductImages().stream()
                                .min(Comparator.comparingInt(ProductImage::getSortOrder))
                                .orElseThrow(() -> new IllegalStateException(
                                        "상품에 이미지가 존재하지 않습니다. 상품ID: " + product.getId()))
                                .getId()
                                .getImageFileId()
                ));

        // 3. file-service 호출해서 이미지 정보 조회
        String imageIdsParam = productToThumbnailId.values().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        ApiResponse<List<ImageFileDTO>> response = fileClient.getImageFilesByIds(imageIdsParam);

        // 4. ID → URL Map
        Map<Long, String> imageIdToUrl = response.getData().stream()
                .collect(Collectors.toMap(ImageFileDTO::getId, ImageFileDTO::getPath));

        // 5. ProductSummaryDto 변환
        return products.stream()
                .map(product -> {
                    Long thumbnailId = productToThumbnailId.get(product.getId());
                    String thumbnailUrl = imageIdToUrl.get(thumbnailId);

                    return ProductSummaryDto.builder()
                            .id(product.getId())
                            .thumbnailUrl(toAbsoluteUrl(thumbnailUrl))
                            .inWishlist(inWishlist(product.getId(), userId))
                            .price(product.getPrice())
                            .emd(product.getTradeAreas().get(0).getArea().getName())
                            .createdAt(product.getCreatedAt())
                            .productStatus(product.getProductStatus().name())
                            .tradeStatus(product.getTradeStatus().name())
                            .isDeleted(product.getIsDeleted())
                            .build();
                })
                .toList();
    }


    // 찜 여부 체크
    private Boolean inWishlist(Long productId, Long userId) {
        if (userId == null) return false; // 로그인 안한 경우
        return wishlistRepository.existsByProductIdAndUserId(productId, userId);
    }
}
