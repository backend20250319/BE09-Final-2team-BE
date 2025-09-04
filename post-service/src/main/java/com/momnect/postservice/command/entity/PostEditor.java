package com.momnect.postservice.command.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "post_editor")
public class PostEditor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 게시글
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // 파일서버에서 받은 파일 ID (없으면 null 가능)
    @Column(name = "file_id")
    private Long fileId;

    // 파일서버에서 받은 public URL
    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    // 사용 여부 (예: 'Y' / 'N')
    @Column(name = "state", length = 1, nullable = false)
    private String state;

    // Post 양방향 연관관계 편의 메서드
    public void setPost(Post post) {
        this.post = post;
    }
}
