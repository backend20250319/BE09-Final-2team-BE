package com.momnect.userservice.command.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MypageDTO {

    private PublicUserDTO profileInfo;
    private List<ChildDTO> childList;
    private TransactionSummaryDTO transactionSummary;
}
