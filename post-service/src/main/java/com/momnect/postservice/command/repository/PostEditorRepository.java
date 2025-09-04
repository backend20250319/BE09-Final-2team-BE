// src/main/java/com/momnect/postservice/command/repository/PostEditorRepository.java
package com.momnect.postservice.command.repository;

import com.momnect.postservice.command.entity.PostEditor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostEditorRepository extends JpaRepository<PostEditor, Long> {
    List<PostEditor> findByPostIdAndState(Long postId, String state);
    List<PostEditor> findByPostId(Long postId);
}
