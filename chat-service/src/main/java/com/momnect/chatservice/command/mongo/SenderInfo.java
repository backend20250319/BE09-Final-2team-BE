package com.momnect.chatservice.command.mongo;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Field;

// 발신자 정보 내장 클래스
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SenderInfo {
    @Field("user_id")
    private Long userId;

    @Field("username")
    private String username;
}
