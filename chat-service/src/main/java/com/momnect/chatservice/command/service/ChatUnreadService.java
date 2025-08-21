package com.momnect.chatservice.command.service;

import com.momnect.chatservice.command.dto.message.RoomUnreadSummaryResponse;
import com.momnect.chatservice.command.dto.message.UnreadCountResponse;

import java.util.List;

public interface ChatUnreadService {
    UnreadCountResponse getUnreadCount(Long roomId, Long userId);
    RoomUnreadSummaryResponse getMyUnreadSummary(Long userId);

    void bumpUnreadForOpponents(Long roomId, Long senderId);   // 상대방들 unread +1
    List<Long> getOpponentsInRoom(Long roomId, Long senderId); // sender 제외한 유저ID 목록
}
