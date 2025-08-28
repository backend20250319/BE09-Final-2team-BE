package com.momnect.chatservice.command.controller;

import com.momnect.chatservice.command.dto.room.ChatRoomCreateRequest;
import com.momnect.chatservice.command.dto.room.ChatRoomParticipantResponse;
import com.momnect.chatservice.command.dto.room.ChatRoomResponse;
import com.momnect.chatservice.command.dto.room.ChatRoomSummaryResponse;
import com.momnect.chatservice.command.service.ChatRoomService;
import com.momnect.chatservice.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    /** 방 생성 (이미 있으면 기존 방 반환) */
    @PostMapping
    public ResponseEntity<ApiResponse<ChatRoomResponse>> create(@Valid @RequestBody ChatRoomCreateRequest req, @AuthenticationPrincipal String userId) {
        try {
            ChatRoomResponse room = chatRoomService.createRoom(req, Long.valueOf(userId));
            return ResponseEntity.ok(ApiResponse.success(room));
        } catch (Exception e) {
            log.error("채팅방 생성 컨트롤러 오류: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.failure("BAD_REQUEST", e.getMessage()));
        }
    }

    /** 내가 참여한 방 목록 (최근 메시지 기준) */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ChatRoomSummaryResponse>>> myRooms(@AuthenticationPrincipal String userId) {
        List<ChatRoomSummaryResponse> rooms = chatRoomService.listRoomsForUser(Long.valueOf(userId));
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    /** 방 참여자 목록 */
    @GetMapping("/{roomId}/participants")
    public ResponseEntity<ApiResponse<List<ChatRoomParticipantResponse>>> participants(@PathVariable Long roomId, @AuthenticationPrincipal String userId) {
        List<ChatRoomParticipantResponse> participants = chatRoomService.getParticipants(roomId, Long.valueOf(userId));
        return ResponseEntity.ok(ApiResponse.success(participants));
    }
}
