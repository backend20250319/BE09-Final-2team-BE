package com.momnect.postservice.command.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageFileDTO {
    private Long id;

    // 다양한 서버 키 대응
    @JsonAlias({"url", "fileUrl", "imageUrl", "publicUrl"})
    private String url;

    // url이 없을 때 경로로 조합
    @JsonAlias({"path", "filePath", "relativePath", "ftpPath", "storedName", "saveName", "savedName"})
    private String path;

    @JsonAlias({"originalFilename", "originalName", "fileName", "filename"})
    private String originalFilename;

    @JsonAlias({"contentType", "mimeType"})
    private String contentType;

    @JsonAlias({"size", "fileSize", "length"})
    private Long size;
}
