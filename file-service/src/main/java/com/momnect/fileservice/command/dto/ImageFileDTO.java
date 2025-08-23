package com.momnect.fileservice.command.dto;

import java.time.LocalDateTime;

import com.momnect.fileservice.command.entity.ImageFile;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ImageFileDTO {

    private Long id;
    private String originalName;
    private String storedName;
    private String path;
    private Long size;
    private String extension;
    private Boolean isDeleted;
    private LocalDateTime createdAt;


    // 엔티티 -> DTO 변환
    public static ImageFileDTO fromEntity(ImageFile entity) {
        return ImageFileDTO.builder()
                .id(entity.getId())
                .originalName(entity.getOriginalName())
                .storedName(entity.getStoredName())
                .path(entity.getPath())
                .size(entity.getSize())
                .extension(entity.getExtension())
                .isDeleted(entity.getIsDeleted())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

