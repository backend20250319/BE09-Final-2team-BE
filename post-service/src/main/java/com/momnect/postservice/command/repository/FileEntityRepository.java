package com.momnect.postservice.command.repository;

import com.momnect.postservice.command.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileEntityRepository extends JpaRepository<FileEntity, Long> {
    Optional<FileEntity> findByPath(String path);
}
