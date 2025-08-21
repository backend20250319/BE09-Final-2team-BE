package com.momnect.chatservice.command.controller;

import com.momnect.chatservice.command.dto.PageResponse;
import com.momnect.chatservice.command.dto.message.ChatMessageMarkReadRequest;
import com.momnect.chatservice.command.dto.message.ChatMessageResponse;
import com.momnect.chatservice.command.dto.message.ChatMessageSendRequest;
import com.momnect.chatservice.command.dto.message.UnreadCountResponse;
import com.momnect.chatservice.command.service.ChatMessageService;
import com.momnect.chatservice.command.service.ChatUnreadService;
import com.momnect.chatservice.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rooms/{roomId}/messages")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final ChatUnreadService chatUnreadService;

    /** 메시지 전송 */
    @PostMapping
    public ResponseEntity<ApiResponse<ChatMessageResponse>> send(
            @PathVariable Long roomId,
            @Valid @RequestBody ChatMessageSendRequest req
    ) {
        ChatMessageResponse sent = chatMessageService.send(roomId, req);
        return ResponseEntity.ok(ApiResponse.success(sent));
    }

    /** 메시지 조회 (최신순 페이지네이션) */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ChatMessageResponse>>> list(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<ChatMessageResponse> content = chatMessageService.getMessages(roomId, page, size);
        PageResponse<ChatMessageResponse> body = PageResponse.<ChatMessageResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .total(-1) // 총 건수 필요시 서비스에서 계산해 채워도 됨
                .build();
        return ResponseEntity.ok(ApiResponse.success(body));
    }

    /** 읽음 처리 */
    @PostMapping("/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @PathVariable Long roomId,
            @Valid @RequestBody ChatMessageMarkReadRequest req
    ) {
        chatMessageService.markAsRead(roomId, req);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** 나의 '해당 방' 안읽은 메시지 수 조회 */
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @PathVariable Long roomId,
            @RequestParam Long userId // 추후 SecurityContext 대체 가능
    ) {
        UnreadCountResponse res = chatUnreadService.getUnreadCount(roomId, userId);
        return ResponseEntity.ok(ApiResponse.success(res));
    }
}
