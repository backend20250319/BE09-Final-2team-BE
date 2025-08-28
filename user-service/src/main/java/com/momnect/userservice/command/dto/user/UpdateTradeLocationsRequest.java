package com.momnect.userservice.command.dto.user;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateTradeLocationsRequest {

    @NotEmpty(message = "하나 이상의 거래 지역을 선택해주세요.")
    @Size(min = 1, max = 3, message = "거래 지역은 1개에서 3개까지 선택할 수 있습니다.")
    private List<Long> areaIds;
}
