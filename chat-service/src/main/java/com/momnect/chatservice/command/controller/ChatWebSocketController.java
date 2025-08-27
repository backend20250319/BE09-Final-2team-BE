package com.momnect.chatservice.command.controller;

import com.momnect.chatservice.command.dto.message.*;
import com.momnect.chatservice.command.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatRoomService chatRoomService;

    @GetMapping("/rooms/{roomId}/members/{userId}")
    public ResponseEntity<MembershipResponse> checkMembership(
            @PathVariable String roomId,
            @PathVariable String userId) {
        
        try {
            // 채팅방 참여자 확인
            boolean isMember = chatRoomService.isParticipant(Long.parseLong(roomId), Long.parseLong(userId));
            
            MembershipResponse response = new MembershipResponse(isMember, roomId, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok(new MembershipResponse(false, roomId, userId));
        }
    }
}
