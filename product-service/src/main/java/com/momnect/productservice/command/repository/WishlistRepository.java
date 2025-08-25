package com.momnect.productservice.command.repository;

import com.momnect.productservice.command.entity.product.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findAllByUserIdAndProductIdIn(Long userId, List<Long> productIds);

    // 특정 유저가 특정 상품을 찜했는지 여부 확인
    boolean existsByProductIdAndUserId(Long productId, Long userId);
}
