package com.momnect.chatservice.command.mongo;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Document(collection = "chat_message")
@CompoundIndex(name = "room_sentAt_idx", def = "{'chat_room_id': 1, 'sent_at': -1}")
public class ChatMessage {

    @Id
    private ObjectId id;

    @Field("chat_room_id")
    private Long chatRoomId;

    @Field("sender_info")
    private SenderInfo senderInfo;

    @Field("message")
    private String message;

    @Field("sent_at")
    private Instant sentAt;

    @Field("is_read")
    private boolean isRead;
}

