package com.momnect.productservice.command.repository;

import com.momnect.productservice.command.entity.product.Product;
import com.momnect.productservice.command.entity.product.RecommendedAge;
import com.momnect.productservice.command.entity.product.TradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 인기 Top30: viewCount DESC, createdAt DESC
    List<Product> findTop30ByIsDeletedFalseAndTradeStatusNotOrderByViewCountDescCreatedAtDesc(TradeStatus tradeStatus);

    // 신규 Top30: createdAt DESC
    List<Product> findTop30ByIsDeletedFalseAndTradeStatusNotOrderByCreatedAtDesc(TradeStatus tradeStatus);

    // 추천 Top30
    List<Product> findTop100ByIsDeletedFalseAndTradeStatusNotAndRecommendedAgeOrderByCreatedAtDescViewCountDesc(TradeStatus tradeStatus, RecommendedAge age);

    // 여러 연령대 IN 조회 + 정렬
    List<Product> findTop100ByIsDeletedFalseAndTradeStatusNotAndRecommendedAgeInOrderByCreatedAtDescViewCountDesc(
            TradeStatus tradeStatus, Collection<RecommendedAge> ages
    );

    // 지정한 ID 목록 조회 (찜순 정렬은 Service에서 ID 순서로 재정렬)
    List<Product> findByIdIn(List<Long> ids);

    List<Product> findTop3BySellerIdOrderByCreatedAtDesc(Long sellerId);

    Integer countByTradeStatusAndSellerIdOrBuyerId(TradeStatus tradeStatus, Long sellerId, Long buyerId);

    Integer countByTradeStatusAndSellerId(TradeStatus tradeStatus, Long userId);

    Integer countByTradeStatusAndBuyerId(TradeStatus tradeStatus, Long userId);

    List<Product> findByTradeStatusAndBuyerId(TradeStatus tradeStatus, Long userId);

    List<Product> findBySellerIdAndIsDeletedFalse(Long userId);

    Integer countBySellerIdAndIsDeletedFalse(Long userId);
}

