package com.momnect.websocketservice.command.feign;

import com.momnect.websocketservice.command.dto.MembershipResponse;
import com.momnect.websocketservice.command.service.ChatService.ApiResponse;
import com.momnect.websocketservice.command.service.ChatService.ChatMessageSendRequest;
import com.momnect.websocketservice.command.service.ChatService.ChatMessageMarkReadRequest;
import com.momnect.websocketservice.command.dto.ChatMessageResponse;
import com.momnect.websocketservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "chat-service", url = "http://54.180.214.196:30080/api/v1/chat-service", configuration = FeignClientConfig.class)
public interface ChatServiceClient {
    
    @GetMapping("/chat/api/rooms/{roomId}/members/{userId}")
    MembershipResponse checkMembership(@PathVariable String roomId, @PathVariable String userId);
    
    @PostMapping("/rooms/{roomId}/messages")
    ApiResponse<ChatMessageResponse> saveMessage(
            @PathVariable String roomId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody ChatMessageSendRequest request);
    
    @PostMapping("/rooms/{roomId}/messages/read")
    ApiResponse<Void> markAsRead(@PathVariable String roomId, @RequestBody ChatMessageMarkReadRequest request);
}
