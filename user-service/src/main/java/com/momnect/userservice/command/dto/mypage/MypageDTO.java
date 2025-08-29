package com.momnect.userservice.command.dto.mypage;

import com.momnect.userservice.command.dto.child.ChildDTO;
import com.momnect.userservice.command.dto.common.TransactionSummaryDTO;
import com.momnect.userservice.command.dto.user.PublicUserDTO;
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
