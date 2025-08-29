package com.momnect.productservice.command.service;

import com.momnect.productservice.command.dto.product.ProductSummaryDto;
import com.momnect.productservice.command.entity.product.Product;
import com.momnect.productservice.command.entity.product.Wishlist;
import com.momnect.productservice.command.repository.ProductRepository;
import com.momnect.productservice.command.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;

    /**
     * 찜 추가
     */
    public void add(Long productId, Long userId) {
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
    public void remove(Long productId, Long userId) {
        Wishlist wishlist = wishlistRepository.findByProductIdAndUserId(productId, userId)
                .orElseThrow(() -> new IllegalArgumentException("찜한 내역이 없습니다."));
        wishlistRepository.delete(wishlist);
    }

    /**
     * 내 찜 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ProductSummaryDto> getMyWishlist(Long userId) {
        List<Wishlist> wishlists = wishlistRepository.findAllByUserId(userId);

        return wishlists.stream()
                .map(w -> ProductSummaryDto.fromEntity(w.getProduct(), "", true))
                .collect(Collectors.toList());
    }
}
