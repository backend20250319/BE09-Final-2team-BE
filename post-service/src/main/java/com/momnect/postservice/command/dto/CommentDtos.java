package com.momnect.postservice.command.dto;

import lombok.*;
import java.time.LocalDateTime;

public class CommentDtos {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class CreateRequest {
        private Long userId;
        private String content;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class DeleteRequest {
        private Long userId;
    }

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class Response {
        private Long id;
        private Long postId;
        private Long userId;
        private String content;
        private LocalDateTime createdAt;
    }
}
