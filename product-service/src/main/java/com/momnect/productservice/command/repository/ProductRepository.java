package com.momnect.productservice.command.repository;

import com.momnect.productservice.command.entity.product.Product;
import com.momnect.productservice.command.entity.product.TradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findTop3BySellerIdOrderByCreatedAtDesc(Long sellerId);

    Integer countByTradeStatusAndSellerIdOrBuyerId(TradeStatus tradeStatus, Long sellerId, Long buyerId);

    Integer countByTradeStatusAndSellerId(TradeStatus tradeStatus, Long userId);

    Integer countByTradeStatusAndBuyerId(TradeStatus tradeStatus, Long userId);
}

