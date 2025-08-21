package com.momnect.chatservice.command.controller;

import com.momnect.chatservice.command.dto.room.ChatRoomCreateRequest;
import com.momnect.chatservice.command.dto.room.ChatRoomParticipantResponse;
import com.momnect.chatservice.command.dto.room.ChatRoomResponse;
import com.momnect.chatservice.command.dto.room.ChatRoomSummaryResponse;
import com.momnect.chatservice.command.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    /** 방 생성 (이미 있으면 기존 방 반환) */
    @PostMapping
    public ResponseEntity<ChatRoomResponse> create(@Valid @RequestBody ChatRoomCreateRequest req) {
        return ResponseEntity.ok(chatRoomService.createRoom(req));
    }

    /** 내가 참여한 방 목록 (최근 메시지 기준) */
    @GetMapping("/me/{userId}")
    public ResponseEntity<List<ChatRoomSummaryResponse>> myRooms(@PathVariable Long userId) {
        return ResponseEntity.ok(chatRoomService.listRoomsForUser(userId));
    }

    /** 방 참여자 목록 */
    @GetMapping("/{roomId}/participants")
    public ResponseEntity<List<ChatRoomParticipantResponse>> participants(@PathVariable Long roomId) {
        return ResponseEntity.ok(chatRoomService.getParticipants(roomId));
    }

    /** 방 단건 조회 */
    @GetMapping("/{roomId}")
    public ResponseEntity<ChatRoomResponse> getRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(chatRoomService.getRoom(roomId));
    }
}
