package com.momnect.chatservice.command.controller;

import com.momnect.chatservice.command.dto.PageResponse;
import com.momnect.chatservice.command.dto.message.ChatMessageMarkReadRequest;
import com.momnect.chatservice.command.dto.message.ChatMessageResponse;
import com.momnect.chatservice.command.dto.message.ChatMessageSendRequest;
import com.momnect.chatservice.command.dto.message.UnreadCountResponse;
import com.momnect.chatservice.command.service.ChatMessageService;
import com.momnect.chatservice.command.service.ChatUnreadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/rooms/{roomId}/messages")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final ChatUnreadService ChatUnreadService;

    /** 메시지 전송 */
    @PostMapping
    public ResponseEntity<ChatMessageResponse> send(
            @PathVariable Long roomId,
            @Valid @RequestBody ChatMessageSendRequest req
    ) {
        return ResponseEntity.ok(chatMessageService.send(roomId, req));
    }

    /** 메시지 조회 (최신순 페이지네이션) */
    @GetMapping
    public ResponseEntity<PageResponse<ChatMessageResponse>> list(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<ChatMessageResponse> content = chatMessageService.getMessages(roomId, page, size);
        return ResponseEntity.ok(PageResponse.<ChatMessageResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .total(-1) // 총 건수 필요 시 별도 계산
                .build());
    }

    /** 읽음 처리 */
    @PostMapping("/read")
    public ResponseEntity<Void> markRead(
            @PathVariable Long roomId,
            @Valid @RequestBody ChatMessageMarkReadRequest req
    ) {
        chatMessageService.markAsRead(roomId, req);
        return ResponseEntity.noContent().build();
    }

    /** 나의 '해당 방' 안읽은 메시지 수 조회 */
    @GetMapping("/unread")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @PathVariable Long roomId,
            @RequestParam Long userId // 추후 SecurityContext로 대체 가능
    ) {
        return ResponseEntity.ok(ChatUnreadService.getUnreadCount(roomId, userId));
    }
}
