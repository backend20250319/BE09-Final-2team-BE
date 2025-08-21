package com.momnect.postservice.command.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Post {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String title;

    @Lob
    @Column(nullable = false)
    private String contentHtml;

    @Column(nullable = false)
    private int viewCount = 0;

    @Column(nullable = false)
    private Boolean hasImage = false;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private PostCategory category;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostEditorFile> editorFiles = new ArrayList<>();

    public void addViews() {
        this.viewCount++;
    }
    public void update(String title, String contentHtml, Boolean hasImage) {
        this.title = title;
        this.contentHtml = contentHtml;
        this.hasImage = hasImage;
    }
    public void softDelete() {
        this.isDeleted = true;
    }

    @Builder
    public Post(Long userId, String title, String contentHtml, Boolean hasImage, PostCategory category) {
        this.userId = userId;
        this.title = title;
        this.contentHtml = contentHtml;
        this.hasImage = hasImage;
        this.category = category;
    }
}

