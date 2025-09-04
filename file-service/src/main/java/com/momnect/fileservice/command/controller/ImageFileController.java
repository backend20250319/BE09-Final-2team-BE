package com.momnect.fileservice.command.controller;

import com.momnect.fileservice.command.dto.ImageFileDTO;
import com.momnect.fileservice.command.service.ImageFileService;
import com.momnect.fileservice.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class ImageFileController {

    private final ImageFileService imageFileService;

    @Value("${ftp.file-server-url}")
    private String fileServerUrl;

    /***
     * 파일 서버 기본 URL 조회
     * @return file-server-url (application.yml에서 가져옴)
     */
    @GetMapping("/server-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> getFileServerUrl() {
        Map<String, String> response = Map.of("fileServiceUrl", fileServerUrl);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /***
     * files?ids=1,2,3
     *
     * 이미지 ID 리스트로 이미지 정보 조회
     * @param ids 조회할 이미지 ID 리스트 (콤마 구분)
     * @return 이미지 정보 DTO 리스트
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ImageFileDTO>>> getImagesByIds(
            @RequestParam(value = "ids") List<Long> ids) {

        List<ImageFileDTO> images = imageFileService.getImagesByIds(ids);
        return ResponseEntity.ok(ApiResponse.success(images));
    }

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

    /***
     * FTP 경로에 있는 이미지 파일 조회
     * @return 해당 디렉토리에 있는 파일 이름 리스트
     */
    @GetMapping("/path")
    public ResponseEntity<ApiResponse<List<String>>> listImages() throws IOException {

        List<String> files = imageFileService.listImages();
        return ResponseEntity.ok(ApiResponse.success(files));
    }

    /***
     * FTP 경로의 모든 파일 삭제
     * @return 삭제된 파일 이름 리스트
     */
    @DeleteMapping("/path")
    public ResponseEntity<ApiResponse<List<String>>> deleteAllFiles() throws IOException {

        List<String> deletedFiles = imageFileService.deleteAllFiles();
        return ResponseEntity.ok(ApiResponse.success(deletedFiles));
    }
}
