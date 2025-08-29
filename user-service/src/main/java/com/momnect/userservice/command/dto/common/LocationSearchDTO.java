package com.momnect.userservice.command.dto.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LocationSearchDTO {

    private Long id;
    private String sido;    // 시도
    private String sigungu; //시군구
    private String emd;     // 읍면동
    private String fullName; // "서울특별시 서대문구 홍제동" 형태
}
