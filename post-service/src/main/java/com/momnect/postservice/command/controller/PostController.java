package com.momnect.postservice.command.controller;

import com.momnect.postservice.common.ApiResponse;
import com.momnect.postservice.command.dto.PostRequestDto;
import com.momnect.postservice.command.dto.PostResponseDto;
import com.momnect.postservice.command.dto.CommentDtos;
import com.momnect.postservice.command.dto.LikeSummaryResponse;
import com.momnect.postservice.command.service.PostService;
import com.momnect.postservice.command.service.CommentService;
import com.momnect.postservice.command.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;
    private final CommentService commentService;
    private final LikeService likeService;

    // form-data: images(File), userId(Text), title(Text), contentHtml(Text), categoryName(Text)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Long> create(
            @RequestParam("userId") Long userId,
            @RequestParam("title") String title,
            @RequestParam("contentHtml") String contentHtml,
            @RequestParam("categoryName") String categoryName,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        PostRequestDto dto = PostRequestDto.builder()
                .userId(userId)
                .title(title)
                .contentHtml(contentHtml) // 본문 내 dataURL은 서비스에서 파일서버 URL로 치환
                .categoryName(categoryName)
                .build();

        Long id = postService.createPost(dto, images);
        return ApiResponse.success(id);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Map<String, Object>> getOne(@PathVariable Long id) {
        PostResponseDto post = postService.getPost(id);
        List<CommentDtos.Response> comments = commentService.listForPost(id);
        LikeSummaryResponse likeSummary = likeService.summary(id);
        return ApiResponse.success(Map.of("post", post, "comments", comments, "like", likeSummary));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Page<PostResponseDto>> list(
            @RequestParam(required = false) String category,
            Pageable pageable
    ) {
        return ApiResponse.success(postService.getPosts(category, pageable));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Long> update(@PathVariable Long id, @RequestBody PostRequestDto dto) {
        postService.updatePost(id, dto);
        return ApiResponse.success(id);
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Long> delete(@PathVariable Long id) {
        postService.deletePost(id);
        return ApiResponse.success(id);
    }
}
