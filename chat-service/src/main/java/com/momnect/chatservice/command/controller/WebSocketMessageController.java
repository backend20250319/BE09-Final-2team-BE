package com.momnect.chatservice.command.controller;
import com.momnect.chatservice.command.dto.message.ChatMessageSendRequest;
import com.momnect.chatservice.command.dto.message.WsSendMessage;
import com.momnect.chatservice.command.service.ChatMessageService;
import com.momnect.chatservice.command.service.ChatUnreadService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WebSocketMessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;  // 이미 REST에서 쓰는 서비스
    private final ChatUnreadService chatUnreadService;

    // 클라이언트 -> 서버: /app/chat.send
    @MessageMapping("/chat.send")
    public void onSend(@Payload WsSendMessage payload) {
        // 1) 영속화 + 응답 변환 (REST와 동일 로직 재사용)
        var saved = chatMessageService.send(payload.getRoomId(),
                new ChatMessageSendRequest(payload.getSenderId(), payload.getMessage()));

        // 2) 방 구독자에게 브로드캐스트 (구독 주소: /topic/rooms.{roomId})
        String roomTopic = "/topic/rooms." + payload.getRoomId();
        messagingTemplate.convertAndSend(roomTopic, saved);

        // 3) 상대방 안읽음 카운트 갱신 후, 개인 큐에 푸시 (선택)
        //   - unreadCountResponse: { roomId, userId, unreadCount }
        chatUnreadService.bumpUnreadForOpponents(payload.getRoomId(), payload.getSenderId());
        var opponents = chatUnreadService.getOpponentsInRoom(payload.getRoomId(), payload.getSenderId());
        for (Long opponentId : opponents) {
            var unreadDto = chatUnreadService.getUnreadCount(payload.getRoomId(), opponentId);
            messagingTemplate.convertAndSendToUser(opponentId.toString(), "/queue/unread", unreadDto);
        }
    }
}
