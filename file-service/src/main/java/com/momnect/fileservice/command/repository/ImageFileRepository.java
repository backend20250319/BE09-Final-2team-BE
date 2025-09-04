package com.momnect.fileservice.command.repository;

import com.momnect.fileservice.command.entity.ImageFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageFileRepository extends JpaRepository<ImageFile, Long> {
}
