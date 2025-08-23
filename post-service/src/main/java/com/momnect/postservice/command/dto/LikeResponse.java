package com.momnect.postservice.command.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeResponse {
    private boolean liked;   // 내 상태
    private long likeCount;  // 총 좋아요 수
}
