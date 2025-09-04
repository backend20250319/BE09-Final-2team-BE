package com.momnect.userservice.command.dto.mypage;

import com.momnect.userservice.command.dto.common.ProductSummaryDTO;
import com.momnect.userservice.command.dto.user.PublicUserDTO;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OtherUserProfileDTO {

    // 1. 프로필 정보 (PublicUserDTO 재사용)
    private PublicUserDTO profileInfo;

    // 2. 거래 현황 요약
    private OtherUserTransactionSummaryDTO transactionSummary;

    // 3. 판매 상품 목록 (ProductSummaryDTO 목록 재사용)
    private List<ProductSummaryDTO> soldProducts;
}
