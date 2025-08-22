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

    // ✅ 상세 조회: post + comments + likeSummary
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getOne(@PathVariable Long id) {
        PostResponseDto post = postService.getPost(id);
        List<CommentDtos.Response> comments = commentService.listForPost(id);
        LikeSummaryResponse likeSummary = likeService.summary(id);

        return ApiResponse.success(Map.of(
                "post", post,
                "comments", comments,
                "like", likeSummary
        ));
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
