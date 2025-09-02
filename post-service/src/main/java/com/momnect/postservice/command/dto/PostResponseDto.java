package com.momnect.postservice.command.dto;

import com.momnect.postservice.command.entity.Post;
import lombok.Data;

import java.util.List;

@Data
public class PostResponseDto {
    private Long id;
    private String title;
    private String contentHtml;
    private Long userId;
    private boolean hasImage;
    private Long coverFileId;

    private List<String> imageUrls;

    public PostResponseDto(Post p) {
        this.id = p.getId();
        this.title = p.getTitle();
        this.contentHtml = p.getContentHtml();
        this.userId = p.getUserId();
        this.hasImage = p.isHasImage();
        this.coverFileId = p.getCoverFileId();
    }
}
