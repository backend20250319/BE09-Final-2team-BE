package com.momnect.postservice.command.client;

import com.momnect.postservice.command.dto.ImageFileDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@FeignClient(
        name = "file-service",
        contextId = "postServiceFileServiceClient",
        url = "${clients.file-service.base-url}")
public interface FileServiceClient{

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    List<ImageFileDTO> upload(@RequestPart("imageFiles") List<MultipartFile> files);

    @GetMapping("/files/server-url")
    String getServerUrl();
}
