package com.momnect.productservice.command.client;

import com.momnect.productservice.command.client.dto.ImageFileDTO;
import com.momnect.productservice.common.ApiResponse;
import com.momnect.productservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// gateway 통해서 접근
@FeignClient(
        name = "file-service",
        configuration = FeignClientConfig.class)
public interface FileClient {

    /***
     * 이미지 파일 목록 조회
     *
     * @param ids 조회할 파일 ID 리스트 (콤마 구분)
     * @return
     */
    @GetMapping("/files")
    ApiResponse<List<ImageFileDTO>> getImageFilesByIds(@RequestParam("ids") String ids);
}
