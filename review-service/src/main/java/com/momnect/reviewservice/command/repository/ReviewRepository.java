package com.momnect.reviewservice.command.repository;

import com.momnect.reviewservice.command.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    // 특정 userId에 해당하는 리뷰의 총 개수를 반환합니다.
    Integer countByUserId(Long userId);
    // 특정 사용자의 리뷰 목록을 조회하는 메서드 (추가)
    List<Review> findByUserId(Long userId);

    /**
     * 명예의 전당을 위한 사용자별 리뷰 통계를 조회합니다.
     * 총 리뷰 개수와 평균 별점을 기준으로 정렬하여 상위 사용자들을 반환합니다.
     */
    @Query(value = """
        SELECT
            r.user_id,
            u.nickname,  
            COUNT(*) as total_reviews,
            AVG(r.rating) as avg_rating
        FROM tbl_review r
        JOIN tbl_user u ON r.user_id = u.id 
        GROUP BY r.user_id, u.nickname
        HAVING COUNT(*) > 0
        ORDER BY COUNT(*) DESC, AVG(r.rating) DESC
        LIMIT 3
        """, nativeQuery = true)

    List<Object[]> findUserStatsForRanking();
}