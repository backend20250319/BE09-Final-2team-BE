package com.momnect.postservice.command.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "post")
public class Post {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 카테고리 매핑 (이미 Long categoryId로 쓰는 중이면 아래를 Long으로 바꾸세요)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private PostCategory category;

    @Column(nullable = false)
    private String title;

    @Column(name = "content_html", nullable = false, columnDefinition = "TEXT")
    private String contentHtml;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Builder.Default
    @Column(name = "has_image", nullable = false)
    private boolean hasImage = false;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Builder.Default
    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ✅ 에디터 파일 매핑 (tbl_post_editor)
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostEditor> editorFiles = new ArrayList<>();

    // DTO에서 getHasImage()를 호출하므로 브릿지 메서드 하나 추가
    public boolean getHasImage() {
        return hasImage;
    }

    // 관계 편의 메서드 (필요하면 사용)
    public void addEditorFile(PostEditor editor) {
        editorFiles.add(editor);
        editor.setPost(this);
    }
}
