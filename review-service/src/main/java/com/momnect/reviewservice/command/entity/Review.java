package com.momnect.reviewservice.command.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_review")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    // reviewId를 기본키로 사용하고, DB의 'id' 컬럼과 매핑합니다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long reviewId;

    private float rating;
    private String content;

//    @Transi
    private String summary;
    private Boolean kind;          // 친절했나요?
    private Boolean promise;       // 약속 잘 지켰나요?
    private Boolean satisfaction;  // 만족했나요?

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
