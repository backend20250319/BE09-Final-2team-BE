package com.momnect.postservice.command.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tbl_post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private PostCategory category;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Lob
    @Column(name = "content_html", nullable = false)
    private String contentHtml;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Builder.Default
    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Builder.Default
    @Column(name = "has_image", nullable = false)
    private boolean hasImage = false;

    @Column(name = "cover_file_id")
    private Long coverFileId;

    // ★ 여기 포인트
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
