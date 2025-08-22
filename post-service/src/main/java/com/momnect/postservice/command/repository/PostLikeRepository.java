package com.momnect.postservice.command.repository;

import com.momnect.postservice.command.entity.PostLike;
import com.momnect.postservice.command.entity.PostLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLikeId> {

    long countByIdPostId(Long postId);

    @Query("select l.id.userId from PostLike l where l.id.postId = :postId")
    List<Long> findUserIdsByPostId(@Param("postId") Long postId);
}
