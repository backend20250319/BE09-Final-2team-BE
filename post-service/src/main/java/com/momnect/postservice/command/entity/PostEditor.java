package com.momnect.postservice.command.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "tbl_post_editor")
public class PostEditor {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    @Setter
    private Post post;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "rename_file_name", nullable = false)
    private String renameFileName;

    @Column(name = "state", nullable = false, length = 1)
    private String state;

    // 컬럼명이 create_at 임에 주의
    @Column(name = "create_at", nullable = false)
    private LocalDateTime createAt;
}
