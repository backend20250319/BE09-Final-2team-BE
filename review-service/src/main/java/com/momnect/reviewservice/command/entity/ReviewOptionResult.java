package com.momnect.reviewservice.command.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tbl_review_option_result")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewOptionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne
    @JoinColumn(name = "option_id", nullable = false)
    private ReviewOption option;

    @Column(name = "flag", nullable = false)
    private Boolean flag;
}