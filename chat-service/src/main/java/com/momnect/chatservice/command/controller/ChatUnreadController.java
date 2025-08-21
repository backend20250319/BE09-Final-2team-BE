// com/momnect/chatservice/command/controller/ChatUnreadController.java
package com.momnect.chatservice.command.controller;

import com.momnect.chatservice.command.dto.message.RoomUnreadSummaryResponse;
import com.momnect.chatservice.command.service.ChatUnreadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ChatUnreadController {

    private final ChatUnreadService chatUnreadService;

    /** 내가 참여한 모든 방의 안읽은 메시지 합계 및 방별 수 */
    @GetMapping("/me/{userId}/unread")
    public ResponseEntity<RoomUnreadSummaryResponse> getMyUnreadSummary(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(chatUnreadService.getMyUnreadSummary(userId));
    }
}
