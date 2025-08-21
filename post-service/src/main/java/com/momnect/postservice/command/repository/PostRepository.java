package com.momnect.postservice.command.repository;

import com.momnect.postservice.command.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
