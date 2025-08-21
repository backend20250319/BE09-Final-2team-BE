// com/momnect/chatservice/command/service/ChatUnreadServiceImpl.java
package com.momnect.chatservice.command.service;

import com.momnect.chatservice.command.dto.message.RoomUnreadSummaryResponse;
import com.momnect.chatservice.command.dto.message.UnreadCountResponse;
import com.momnect.chatservice.command.entity.ChatParticipant;
import com.momnect.chatservice.command.repository.ChatParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatUnreadServiceImpl implements ChatUnreadService {

    private final ChatParticipantRepository participantRepository;

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long roomId, Long userId) {
        ChatParticipant p = participantRepository.findFirstByChatRoomIdAndUserId(roomId, userId);
        int count = (p != null) ? p.getUnreadCount() : 0;

        return UnreadCountResponse.builder()
                .roomId(roomId)
                .userId(userId)
                .unreadCount(count)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RoomUnreadSummaryResponse getMyUnreadSummary(Long userId) {
        var parts = participantRepository.findByUserId(userId);
        int total = parts.stream().mapToInt(ChatParticipant::getUnreadCount).sum();

        var rooms = parts.stream()
                .map(p -> RoomUnreadSummaryResponse.RoomUnread.builder()
                        .roomId(p.getChatRoomId())
                        .unreadCount(p.getUnreadCount())
                        .build())
                .collect(Collectors.toList());

        return RoomUnreadSummaryResponse.builder()
                .userId(userId)
                .totalUnread(total)
                .rooms(rooms)
                .build();
    }

    /**
     * 발신자를 제외한 참여자들의 unreadCount를 +1
     */
    @Override
    @Transactional
    public void bumpUnreadForOpponents(Long roomId, Long senderId) {
        List<ChatParticipant> parts = participantRepository.findByChatRoomId(roomId);
        for (ChatParticipant p : parts) {
            if (!p.getUserId().equals(senderId)) {
                // 도메인 메서드가 있으면 사용:
                p.increaseUnreadCount();
            }
        }
        participantRepository.saveAll(parts);
    }

    /**
     * 방 참여자 중 sender를 제외한 유저ID 목록
     */
    @Override
    @Transactional(readOnly = true)
    public List<Long> getOpponentsInRoom(Long roomId, Long senderId) {
        return participantRepository.findByChatRoomId(roomId).stream()
                .map(ChatParticipant::getUserId)
                .filter(id -> !id.equals(senderId))
                .distinct()
                .collect(Collectors.toList());
    }
}
