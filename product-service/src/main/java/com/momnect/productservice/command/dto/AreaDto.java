package com.momnect.productservice.command.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AreaDto {
    private Integer id;
    private String fullName;
}
