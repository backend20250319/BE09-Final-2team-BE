package com.momnect.postservice.command.controller;

import com.momnect.postservice.command.dto.LikeResponse;
import com.momnect.postservice.command.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts/{postId}/likes")
public class LikeController {

    private final LikeService likeService;

    public record LikeToggleRequest(Long userId) {}

    // 좋아요
    @PostMapping
    public ResponseEntity<LikeResponse> like(
            @PathVariable Long postId,
            @RequestBody LikeToggleRequest req
    ) {
        return ResponseEntity.ok(likeService.like(postId, req.userId()));
    }

    // 좋아요 취소
    @DeleteMapping
    public ResponseEntity<LikeResponse> unlike(
            @PathVariable Long postId,
            @RequestBody LikeToggleRequest req
    ) {
        return ResponseEntity.ok(likeService.unlike(postId, req.userId()));
    }
}
