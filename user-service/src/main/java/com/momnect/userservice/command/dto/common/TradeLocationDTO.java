package com.momnect.userservice.command.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeLocationDTO {
    private Long id;
    private String sido;    //시도
    private String sigungu; //시군구
    private String emd;     //읍면동
}
