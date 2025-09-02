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

import java.time.LocalDateTime;
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

    private final ProductService productService; // 주입받기

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

        return productService.toProductSummaryDtos(products, userId);
    }

    /**
     * 내 판매 상품 조회
     */
    @Transactional(readOnly = true)
    public List<ProductSummaryDto> getMySales(Long userId) {
        List<Product> products = productRepository.findBySellerIdAndIsDeletedFalse(userId);

        return productService.toProductSummaryDtos(products, userId);
    }

    /**
     * 특정 유저 판매 상품 조회
     */
    @Transactional(readOnly = true)
    public List<ProductSummaryDto> getUserSales(Long userId, Long sellerId) {
        List<Product> products = productRepository.findBySellerIdAndIsDeletedFalse(sellerId);

        return productService.toProductSummaryDtos(products, userId);
    }

    /**
     * 상품 판매 완료 처리
     */
    public void completeSale(Long productId, Long sellerId, Long buyerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + productId));

        // 판매자 권한 체크
        if (!product.getSellerId().equals(sellerId)) {
            throw new IllegalStateException("판매자만 판매 완료 처리가 가능합니다.");
        }

        // 이미 판매 완료된 상품 체크
        if (product.getTradeStatus() == TradeStatus.SOLD) {
            throw new IllegalStateException("이미 판매 완료된 상품입니다.");
        }

        // 거래 상태 변경
        LocalDateTime now = LocalDateTime.now();
        product.setTradeStatus(TradeStatus.SOLD);
        product.setBuyerId(buyerId);
        product.setSoldAt(now);

        // 업데이트 정보 갱신
        product.setUpdatedAt(now);
        product.setUpdatedBy(sellerId);

        productRepository.save(product);
    }


    /**
     * 상품 거래 상태 변경
     */
    @Transactional
    public void updateTradeStatus(Long productId, Long userId, TradeStatus newStatus) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + productId));

        // 이미 판매 완료된 상품은 상태 변경 불가
        if (product.getTradeStatus() == TradeStatus.SOLD) {
            throw new IllegalStateException("판매 완료된 상품은 상태를 변경할 수 없습니다.");
        }

        // 권한 체크: 판매자만 상태 변경 가능
        if (!product.getSellerId().equals(userId)) {
            throw new IllegalStateException("판매자만 상품 거래 상태를 변경할 수 있습니다.");
        }

        // 상태 변경
        product.setTradeStatus(newStatus);

        // 업데이트 정보 갱신
        LocalDateTime now = LocalDateTime.now();
        product.setUpdatedAt(now);
        product.setUpdatedBy(userId);

        productRepository.save(product);
    }

    // 찜 여부 체크
    private Boolean inWishlist(Long productId, Long userId) {
        if (userId == null) return false; // 로그인 안한 경우
        return wishlistRepository.existsByProductIdAndUserId(productId, userId);
    }
}
