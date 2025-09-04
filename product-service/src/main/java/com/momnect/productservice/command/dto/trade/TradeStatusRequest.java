package com.momnect.productservice.command.dto.trade;

import com.momnect.productservice.command.entity.product.TradeStatus;
import lombok.Getter;

@Getter
public class TradeStatusRequest {
    private TradeStatus newStatus;
}
