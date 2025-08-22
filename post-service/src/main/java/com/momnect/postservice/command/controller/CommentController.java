package com.momnect.postservice.command.controller;

import com.momnect.postservice.command.dto.CommentDtos;
import com.momnect.postservice.command.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 댓글 작성
    @PostMapping
    public ResponseEntity<CommentDtos.Response> create(
            @PathVariable Long postId,
            @RequestBody CommentDtos.CreateRequest req
    ) {
        var userId = req.getUserId();
        var res = commentService.create(postId, userId, userId, req.getContent());
        return ResponseEntity.ok(res);
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody CommentDtos.DeleteRequest req
    ) {
        commentService.delete(postId, commentId, req.getUserId());
        return ResponseEntity.noContent().build();
    }

}
