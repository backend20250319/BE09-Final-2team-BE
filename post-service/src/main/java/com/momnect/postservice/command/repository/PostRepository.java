package com.momnect.postservice.command.repository;

import com.momnect.postservice.command.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByIsDeletedFalse(Pageable pageable);

    Page<Post> findByCategory_NameAndIsDeletedFalse(String categoryName, Pageable pageable);
}
