package com.momnect.postservice.command.controller;

import com.momnect.postservice.common.ApiResponse;
import com.momnect.postservice.command.dto.PostRequestDto;
import com.momnect.postservice.command.dto.PostResponseDto;
import com.momnect.postservice.command.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    /**
     * ★ multipart/form-data용 게시글 작성
     *
     * (Postman body → form-data 로 전송 시 필드 하나씩 분리해서 입력)
     *
     * userId        - Text
     * title         - Text
     * contentHtml   - Text
     * categoryName  - Text
     * images        - File (1개 또는 N개)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Long> create(
            @RequestParam("userId") Long userId,
            @RequestParam("title") String title,
            @RequestParam("contentHtml") String contentHtml,
            @RequestParam("categoryName") String categoryName,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        PostRequestDto dto = new PostRequestDto();
        dto.setUserId(userId);
        dto.setTitle(title);
        dto.setContentHtml(contentHtml);
        dto.setCategoryName(categoryName);
        dto.setHasImage(images != null && !images.isEmpty());

        Long id = postService.createPost(dto, images);
        return ApiResponse.success(id);
    }

    @GetMapping("/{id}")
    public ApiResponse<PostResponseDto> getOne(@PathVariable Long id) {
        return ApiResponse.success(postService.getPost(id));
    }

    @GetMapping
    public ApiResponse<Page<PostResponseDto>> list(
            @RequestParam(required = false) String category,
            Pageable pageable
    ) {
        return ApiResponse.success(postService.getPosts(category, pageable));
    }

    @PutMapping("/{id}")
    public ApiResponse<Long> update(@PathVariable Long id,
                                    @RequestBody PostRequestDto dto) {
        postService.updatePost(id, dto);
        return ApiResponse.success(id);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Long> delete(@PathVariable Long id) {
        postService.deletePost(id);
        return ApiResponse.success(id);
    }
}
