package com.momnect.postservice.command.client;

import com.momnect.postservice.common.ApiResponse;
import com.momnect.postservice.command.dto.ImageFileDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "file-service",
        // Eureka 쓰면 url 제거, 게이트웨이/직통이면 아래 프로퍼티 사용
        url = "${clients.file-service.base-url}",
        configuration = com.momnect.postservice.config.FeignMultipartConfig.class
)
public interface FileServiceClient {

    // file-service 컨트롤러의 파트명과 동일하게 "imageFiles"
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<List<ImageFileDTO>> upload(@RequestPart("imageFiles") MultipartFile file);

    // 기본 파일 서버 URL 조회
    @GetMapping("/files/server-url")
    ApiResponse<Map<String, String>> getServerUrl();
}
