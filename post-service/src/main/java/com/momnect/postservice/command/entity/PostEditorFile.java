package com.momnect.postservice.command.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_post_editor")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostEditorFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "rename_file_name", nullable = false)
    private String renameFileName;

    @Column(name = "create_at", nullable = false)
    private LocalDateTime createAt;

    @Column(name = "state", nullable = false)
    private char state; // 'Y':정상, 'N':삭제

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Builder
    public PostEditorFile(String originalFileName, String renameFileName, Post post) {
        this.originalFileName = originalFileName;
        this.renameFileName = renameFileName;
        this.post = post;
        this.createAt = LocalDateTime.now();
        this.state = 'Y';
    }

    // 소프트 삭제
    public void delete() {
        this.state = 'N';
    }
}
