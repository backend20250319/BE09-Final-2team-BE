package com.momnect.postservice.command.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostEditorFileDto {
    private Long id;  // 첨부파일 PK만 반환
}
