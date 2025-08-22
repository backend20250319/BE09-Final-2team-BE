package com.momnect.fileservice.command.controller;

import com.momnect.fileservice.command.dto.ImageFileDTO;
import com.momnect.fileservice.command.service.ImageFileService;
import com.momnect.fileservice.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class ImageFileController {

    private final ImageFileService imageFileService;

    /***
     * 파일 업로드
     * @param files 업로드할 이미지 파일 리스트
     * @return 업로드된 이미지 파일 dto 리스트
     */
    @PostMapping
    public ResponseEntity<ApiResponse<List<ImageFileDTO>>> upload(@RequestParam("imageFiles") List<MultipartFile> files)
            throws IOException {

        List<ImageFileDTO> imageFiles = imageFileService.upload(files);
        return ResponseEntity.ok(ApiResponse.success(imageFiles));
    }
}
