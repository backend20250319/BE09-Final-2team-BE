package com.momnect.postservice.command.repository;

import com.momnect.postservice.command.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByCategory_Id(Long categoryId, Pageable pageable);
}
