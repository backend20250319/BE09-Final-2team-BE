package com.momnect.userservice.command.client;

import com.momnect.userservice.command.dto.file.ImageFileDTO;
import com.momnect.userservice.common.ApiResponse;
import com.momnect.userservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@FeignClient(name = "file-service", configuration = FeignClientConfig.class)
public interface FileClient {

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<List<ImageFileDTO>> uploadImage(@RequestPart("imageFiles") MultipartFile file);

    @GetMapping("/files")
    ApiResponse<List<ImageFileDTO>> getImagesByIds(@RequestParam("ids") String ids);

    @DeleteMapping("/files/{id}")
    ApiResponse<Void> deleteImage(@PathVariable("id") Long id);
}
