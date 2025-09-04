package com.momnect.postservice.command.dto;

import lombok.Data;

@Data
public class ImageFileDTO {
    private Long id;
    private Long fileId;
    private String url;
    private String path;
    private String filename;

    private String contentType;
    private Long size;
}
