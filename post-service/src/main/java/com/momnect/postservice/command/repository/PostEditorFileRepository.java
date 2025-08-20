package com.momnect.postservice.command.repository;

import com.momnect.postservice.command.entity.PostEditorFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostEditorFileRepository extends JpaRepository<PostEditorFile, Long> {
}
