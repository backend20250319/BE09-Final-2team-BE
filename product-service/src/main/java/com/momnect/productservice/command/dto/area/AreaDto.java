package com.momnect.productservice.command.dto.area;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AreaDto {
    private Integer id;
    private String emd;
    private String fullName;
}
