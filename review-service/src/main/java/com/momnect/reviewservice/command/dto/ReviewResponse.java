package com.momnect.reviewservice.command.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long reviewId;
    private float rating;
    private String content;
    private String summary;
    private Boolean kind;
    private Boolean promise;
    private Boolean satisfaction;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String sentiment;

}
