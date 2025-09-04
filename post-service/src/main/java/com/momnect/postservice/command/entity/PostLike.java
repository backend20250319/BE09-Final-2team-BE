package com.momnect.postservice.command.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_post_like")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostLike {

    @EmbeddedId
    private PostLikeId id;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // 편의 접근자 (선택)
    public Long getPostId() { return id != null ? id.getPostId() : null; }
    public Long getUserId() { return id != null ? id.getUserId() : null; }
}
