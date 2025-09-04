package com.momnect.reviewservice.command.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tbl_review_sentiment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSentiment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "review_id", nullable = false, unique = true)
    private Review review;

    @Column(name = "sentiment", nullable = false, length = 10)
    private String sentiment; // "긍정적" 또는 "부정적"
}