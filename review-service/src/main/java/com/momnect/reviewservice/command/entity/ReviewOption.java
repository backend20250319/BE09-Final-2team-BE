package com.momnect.reviewservice.command.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_review_option")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long optionId;

    @Column(name = "name", nullable = false, length = 20)
    private String name;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}