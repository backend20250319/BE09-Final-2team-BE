package com.momnect.postservice.command.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class ImageFileDTO {
    @JsonAlias({"url","fileUrl","imageUrl","publicUrl"})
    private String url;
    private String path;
    private String filename;
    private String contentType;
    private Long size;
}
