package com.momnect.postservice.command.dto;

import com.momnect.postservice.command.entity.Post;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class PostResponseDto {

    private Long id;
    private Long userId;
    private String categoryName;
    private String title;
    private String contentHtml;
    private int viewCount;
    private Boolean hasImage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PostEditorFileDto> files;  

    public PostResponseDto(Post post) {
        this.id = post.getId();
        this.userId = post.getUserId();
        this.categoryName = post.getCategory().getName();
        this.title = post.getTitle();
        this.contentHtml = post.getContentHtml();
        this.viewCount = post.getViewCount();
        this.hasImage = post.getHasImage();
        this.createdAt = post.getCreatedAt();
        this.updatedAt = post.getUpdatedAt();

        // 파일이 있는 경우(ID만)
        this.files = post.getEditorFiles().stream()
                .filter(f -> "Y".equalsIgnoreCase(f.getState()))
                .map(f -> new PostEditorFileDto(f.getId()))
                .toList();
    }
}
