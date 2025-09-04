package com.momnect.postservice.command.repository;

import com.momnect.postservice.command.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdAndStatusOrderByCreatedAtAsc(Long postId, Comment.Status status);
}
