package com.momnect.postservice.command.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@IdClass(PostImageId.class)
@Table(name = "tbl_post_image")
public class PostImage {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Id
    @Column(name = "image_file_id")
    private Long imageFileId;
}
