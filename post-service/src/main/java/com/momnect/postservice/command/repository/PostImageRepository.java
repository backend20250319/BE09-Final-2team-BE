package com.momnect.postservice.command.repository;

import com.momnect.postservice.command.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Object> {

    @Query(value = """
        select f.path
        from tbl_post_image pi
        join tbl_image_file f on f.id = pi.image_file_id
        where pi.post_id = :postId and f.is_deleted = 0
        order by pi.image_file_id
        """, nativeQuery = true)
    List<String> findImagePathsByPostId(@Param("postId") Long postId);
}
