package com.momnect.userservice.command.dto.file;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ImageFileDTO {

    private Long id;
    private String originalName;        // 원본 이미지 파일명
    private String storedName;          // 파일 서버에 저장된 파일명
    private String path;                // 파일이 저장된 경로
    private Long size;                  // 파일 크기 (바이트)
    private String extension;           // 파일 확장자 (예: jpg, png)
    private Boolean isDeleted;          // 삭제 여부
    private LocalDateTime createdAt;    // 생성 일시
}
