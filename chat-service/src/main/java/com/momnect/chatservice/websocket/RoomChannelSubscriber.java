// com/momnect/chatservice/websocket/RoomChannelSubscriber.java
package com.momnect.chatservice.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;           // ✅ redis Message
import org.springframework.data.redis.connection.MessageListener; // ✅ implements
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class RoomChannelSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onMessage(Message message, byte[] pattern) { // ✅ public + 정확한 시그니처
        String json = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            // 필요 DTO로 파싱 (여기서는 문자열 그대로 브로드캐스트 예시)
            // WsSendMessage evt = objectMapper.readValue(json, WsSendMessage.class);
            // String dest = "/topic/rooms." + evt.getRoomId();
            // messagingTemplate.convertAndSend(dest, evt);

            // evt 구조 확정 전이면 채널만 보고 브로드캐스트 해도 됨
            String channel = message.getChannel() == null ? "" :
                    new String(message.getChannel(), StandardCharsets.UTF_8);
            // 예: channel:room:123 → 토픽 "/topic/rooms.123"
            String dest = "/topic/" + channel.replace("channel:", "").replace(':','.');
            messagingTemplate.convertAndSend(dest, json);
        } catch (Exception e) {
            // log.warn("Redis pub/sub parse error", e);
        }
    }
}
