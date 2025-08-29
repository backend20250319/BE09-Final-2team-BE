package com.momnect.postservice.command.controller;

import com.momnect.postservice.common.ApiResponse;
import com.momnect.postservice.command.client.FileServiceClient;
import com.momnect.postservice.command.dto.ImageFileDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts/images")
public class PostImageController {

    private final FileServiceClient fileServiceClient;

    @PostMapping(value = "/editor-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<List<ImageFileDTO>> uploadForEditor(@RequestPart("upload") MultipartFile file) {
        // Feign은 List<MultipartFile> 시그니처 → List.of(...)로 감싸 전달
        List<ImageFileDTO> resp = fileServiceClient.upload(List.of(file));
        return ApiResponse.success(resp);
    }

    @GetMapping(value = "/server-url", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Map<String, String>> getServerUrl() {
        String baseUrl = fileServiceClient.getServerUrl();
        return ApiResponse.success(Map.of("baseUrl", baseUrl));
    }
}
