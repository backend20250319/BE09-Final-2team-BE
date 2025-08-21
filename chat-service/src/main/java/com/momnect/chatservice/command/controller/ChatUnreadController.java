package com.momnect.chatservice.command.controller;

import com.momnect.chatservice.command.dto.message.RoomUnreadSummaryResponse;
import com.momnect.chatservice.command.service.ChatUnreadService;
import com.momnect.chatservice.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ChatUnreadController {

    private final ChatUnreadService chatUnreadService;

    /** 내가 참여한 모든 방의 안읽은 메시지 합계 및 방별 수 */
    @GetMapping("/me/{userId}/unread")
    public ResponseEntity<ApiResponse<RoomUnreadSummaryResponse>> getMyUnreadSummary(
            @PathVariable Long userId
    ) {
        RoomUnreadSummaryResponse summary = chatUnreadService.getMyUnreadSummary(userId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
