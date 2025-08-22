package com.momnect.postservice.command.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_comment")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@SQLRestriction("status = 'ACTIVE'") // 기본 조회는 활성만
public class Comment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long postId;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false, length = 1000) private String content;

    @Column(nullable = false) private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Column(nullable = false) private Long createdBy;
    private Long updatedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status;

    private LocalDateTime deletedAt;

    public enum Status { ACTIVE, DELETED }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        status = status == null ? Status.ACTIVE : status;
    }

    public void softDelete(Long actorId) {
        this.status = Status.DELETED;
        this.deletedAt = LocalDateTime.now();
        this.updatedBy = actorId;
    }
}
