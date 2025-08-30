package com.momnect.productservice.command.repository;

import com.momnect.productservice.command.entity.product.Wishlist;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    Optional<Wishlist> findByProductIdAndUserId(Long productId, Long userId);

    List<Wishlist> findAllByUserId(Long userId);

    // 특정 유저가 특정 상품을 찜했는지 여부 확인
    boolean existsByProductIdAndUserId(Long productId, Long userId);

    // 찜수가 많은 상품 TOP N
    @Query("select w.product.id from Wishlist w group by w.product.id order by count(w.id) desc")
    List<Long> findTopProductIdsByLikeCount(Pageable pageable);

    @Query("select w.product.id from Wishlist w where w.userId = :userId")
    Set<Long> findProductIdsByUserId(Long userId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    long countByProductId(Long productId);
}
