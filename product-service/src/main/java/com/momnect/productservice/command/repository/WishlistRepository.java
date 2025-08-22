package com.momnect.productservice.command.repository;

import com.momnect.productservice.command.entity.product.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findAllByUserIdAndProductIdIn(Long userId, List<Long> productIds);
}
