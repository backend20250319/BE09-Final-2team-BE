package com.momnect.reviewservice.command.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {
    private Long userId;
    private Long productId;
    private float rating;
    private String content;
    private Boolean kind;
    private Boolean promise;
    private Boolean satisfaction;
}
