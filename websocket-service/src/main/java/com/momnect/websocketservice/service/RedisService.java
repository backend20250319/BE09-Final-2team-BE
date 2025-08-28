package com.momnect.websocketservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    // 언리드 카운트 관리
    public void incrementUnreadCount(String roomId, String userId) {
        String key = String.format("room:%s:unread:%s", roomId, userId);
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofDays(30)); // 30일 만료
        log.info("Incremented unread count for room: {}, user: {}", roomId, userId);
    }

    public void resetUnreadCount(String roomId, String userId) {
        String key = String.format("room:%s:unread:%s", roomId, userId);
        redisTemplate.delete(key);
        log.info("Reset unread count for room: {}, user: {}", roomId, userId);
    }

    public int getUnreadCount(String roomId, String userId) {
        String key = String.format("room:%s:unread:%s", roomId, userId);
        String count = redisTemplate.opsForValue().get(key);
        return count != null ? Integer.parseInt(count) : 0;
    }

    // Presence 관리
    public void setUserOnline(String userId) {
        String key = String.format("presence:online:%s", userId);
        redisTemplate.opsForValue().set(key, "online", Duration.ofMinutes(5)); // 5분 만료
        log.info("User {} is now online", userId);
    }

    public void setUserOffline(String userId) {
        String key = String.format("presence:online:%s", userId);
        redisTemplate.delete(key);
        log.info("User {} is now offline", userId);
    }

    public boolean isUserOnline(String userId) {
        String key = String.format("presence:online:%s", userId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
