package com.momnect.productservice.command.service;

import com.momnect.productservice.command.dto.trade.TradeSummaryDTO;
import com.momnect.productservice.command.entity.product.TradeStatus;
import com.momnect.productservice.command.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public TradeSummaryDTO getTradeSummary(Long userId, boolean isMyProfile) {
        Integer salesCount = productRepository.countByTradeStatusAndSellerId(
                TradeStatus.SOLD, userId);

        if (isMyProfile) {
            Integer purchaseCount = productRepository.countByTradeStatusAndBuyerId(
                    TradeStatus.SOLD, userId);
            return TradeSummaryDTO.builder().salesCount(salesCount).purchaseCount(purchaseCount).build();
        } else {
            // 타유저 거래 현황은 판매수만 리턴
            return TradeSummaryDTO.builder().salesCount(salesCount).build();
        }
    }
}
