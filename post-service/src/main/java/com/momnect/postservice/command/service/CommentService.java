package com.momnect.postservice.command.service;

import com.momnect.postservice.command.dto.CommentDtos;
import com.momnect.postservice.command.entity.Comment;
import com.momnect.postservice.command.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;

    @Transactional
    public CommentDtos.Response create(Long postId, Long userId, Long actorId, String content) {
        Comment saved = commentRepository.save(
                Comment.builder()
                        .postId(postId)
                        .userId(userId)
                        .content(content)
                        .createdBy(actorId)
                        .status(Comment.Status.ACTIVE) // 안전장치
                        .build()
        );
        return CommentDtos.Response.builder()
                .id(saved.getId())
                .postId(saved.getPostId())
                .userId(saved.getUserId())
                .content(saved.getContent())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional
    public void delete(Long postId, Long commentId, Long actorId) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("comment not found"));

        if (!c.getPostId().equals(postId)) {
            throw new IllegalArgumentException("comment does not belong to post");
        }
        if (!c.getUserId().equals(actorId)) {
            throw new SecurityException("no permission to delete this comment");
        }
        c.softDelete(actorId); // status=DELETED, deletedAt=now
    }

    // 게시글 상세에서 사용할 댓글 목록
    @Transactional(readOnly = true)
    public List<CommentDtos.Response> listForPost(Long postId) {
        var rows = commentRepository
                .findByPostIdAndStatusOrderByCreatedAtAsc(postId, Comment.Status.ACTIVE);

        return rows.stream()
                .map(c -> CommentDtos.Response.builder()
                        .id(c.getId())
                        .postId(c.getPostId())
                        .userId(c.getUserId())
                        .content(c.getContent())
                        .createdAt(c.getCreatedAt())
                        .build())
                .toList();
    }
}
