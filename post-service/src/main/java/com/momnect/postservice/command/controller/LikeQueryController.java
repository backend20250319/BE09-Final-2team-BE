package com.momnect.postservice.command.controller;

import com.momnect.postservice.command.dto.LikeSummaryResponse;
import com.momnect.postservice.command.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class LikeQueryController {

    private final LikeService likeService;

    @GetMapping("/{postId}/like")
    public ResponseEntity<LikeSummaryResponse> getLikeSummary(@PathVariable Long postId) {
        return ResponseEntity.ok(likeService.summary(postId));
    }
}
