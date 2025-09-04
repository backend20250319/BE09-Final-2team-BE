package com.momnect.postservice.command.repository;

import com.momnect.postservice.command.entity.PostCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostCategoryRepository extends JpaRepository<PostCategory, Long> {
    Optional<PostCategory> findTopByNameOrderByIdAsc(String name);
}
