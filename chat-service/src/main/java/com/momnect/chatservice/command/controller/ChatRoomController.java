package com.momnect.chatservice.command.controller;

import com.momnect.chatservice.command.dto.room.ChatRoomCreateRequest;
import com.momnect.chatservice.command.dto.room.ChatRoomParticipantResponse;
import com.momnect.chatservice.command.dto.room.ChatRoomResponse;
import com.momnect.chatservice.command.dto.room.ChatRoomSummaryResponse;
import com.momnect.chatservice.command.service.ChatRoomService;
import com.momnect.chatservice.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    /** 방 생성 (이미 있으면 기존 방 반환) */
    @PostMapping
    public ResponseEntity<ApiResponse<ChatRoomResponse>> create(@Valid @RequestBody ChatRoomCreateRequest req) {
        ChatRoomResponse room = chatRoomService.createRoom(req);
        return ResponseEntity.ok(ApiResponse.success(room));
    }

    /** 내가 참여한 방 목록 (최근 메시지 기준) */
    @GetMapping("/me/{userId}")
    public ResponseEntity<ApiResponse<List<ChatRoomSummaryResponse>>> myRooms(@PathVariable Long userId) {
        List<ChatRoomSummaryResponse> rooms = chatRoomService.listRoomsForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    /** 방 참여자 목록 */
    @GetMapping("/{roomId}/participants")
    public ResponseEntity<ApiResponse<List<ChatRoomParticipantResponse>>> participants(@PathVariable Long roomId) {
        List<ChatRoomParticipantResponse> participants = chatRoomService.getParticipants(roomId);
        return ResponseEntity.ok(ApiResponse.success(participants));
    }
}
