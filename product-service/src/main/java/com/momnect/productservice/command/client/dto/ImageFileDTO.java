package com.momnect.productservice.command.client.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

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
}

