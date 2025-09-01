package com.momnect.postservice.command.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PostImageId implements Serializable {

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "image_file_id")
    private Long imageFileId;
    public PostImageId() {}
    public PostImageId(Long postId, Long imageFileId) {
        this.postId = postId;
        this.imageFileId = imageFileId;
    }

    public Long getPostId() { return postId; }
    public Long getImageFileId() { return imageFileId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public void setImageFileId(Long imageFileId) { this.imageFileId = imageFileId; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostImageId that)) return false;
        return Objects.equals(postId, that.postId) &&
                Objects.equals(imageFileId, that.imageFileId);
    }
    @Override public int hashCode() { return Objects.hash(postId, imageFileId); }
}
