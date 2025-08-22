package com.momnect.postservice.command.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class LikeSummaryResponse {
    private long likeCount;      // 총 좋아요 수
    private List<Long> userIds;  // 좋아요 누른 사용자 ID 목록
}
