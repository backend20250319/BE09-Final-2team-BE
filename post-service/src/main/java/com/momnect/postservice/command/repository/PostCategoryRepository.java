package com.momnect.postservice.command.repository;

import com.momnect.postservice.command.entity.PostCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostCategoryRepository extends JpaRepository<PostCategory, Long> {
    PostCategory findByName(String name);
}
