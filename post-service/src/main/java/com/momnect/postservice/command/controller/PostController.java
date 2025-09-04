package com.momnect.postservice.command.controller;

import com.momnect.postservice.command.client.UserClient;
import com.momnect.postservice.command.client.dto.PublicUserDTO;
import com.momnect.postservice.command.dto.CommentDtos;
import com.momnect.postservice.command.dto.LikeSummaryResponse;
import com.momnect.postservice.command.dto.PostRequestDto;
import com.momnect.postservice.command.dto.PostResponseDto;
import com.momnect.postservice.command.service.CommentService;
import com.momnect.postservice.command.service.LikeService;
import com.momnect.postservice.command.service.PostService;
import com.momnect.postservice.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;
    private final CommentService commentService;
    private final LikeService likeService;
    private final UserClient userClient;

    private static final String[] FILE_PART_NAMES = new String[]{
            "file", "files", "image", "images", "multipartFile",
            "uploadFile", "uploadFiles", "imageFile", "imageFiles", "files[]"
    };

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ApiResponse<Long> create(
            @AuthenticationPrincipal String userId,
            @RequestParam("title") String title,
            @RequestParam("contentHtml") String contentHtml,
            @RequestParam("categoryName") String categoryName,
            MultipartHttpServletRequest multipartRequest // 파트 이름 유연하게 처리
    ) {

        log.info("[create] ==================>>> {}", userId);
        // 1) 멀티파트에서 지원하는 모든 파트 이름을 모아 파일 리스트로 정규화
        List<MultipartFile> images = collectFiles(multipartRequest);

        // 2) 로깅
        if (CollectionUtils.isEmpty(images)) {
            log.info("[create] images=empty (no file parts found)");
        } else {
            log.info("[create] images size={}", images.size());
            for (MultipartFile f : images) {
                log.info(" - fn={}, size={}, type={}",
                        safeName(f.getOriginalFilename()), f.getSize(), f.getContentType());
            }
        }

        // 3) DTO 구성
        PostRequestDto dto = new PostRequestDto();
        dto.setUserId(Long.valueOf(userId));
        dto.setTitle(title);
        dto.setContentHtml(contentHtml);
        if(categoryName.equals("tips")){
            categoryName = "Tip";
        }
        dto.setCategoryName(categoryName);
        dto.setHasImage(!CollectionUtils.isEmpty(images));

        // 4) 서비스 호출
        Long id = postService.createPost(dto, images);
        return ApiResponse.success(id);
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getOne(@PathVariable Long id) {
        PostResponseDto post = postService.getPost(id);
        ResponseEntity<ApiResponse<PublicUserDTO>> userInfo = userClient.getBasicInfo(id);
        log.info("[userInfo] =======> {}", userInfo);
        post.setNickName(userInfo.getBody().getData().getNickname());
        List<CommentDtos.Response> comments = commentService.listForPost(id);
        LikeSummaryResponse likeSummary = likeService.summary(id);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("post", post);
        payload.put("comments", comments);
        payload.put("like", likeSummary);
        return ApiResponse.success(payload);
    }

    @GetMapping
    public ApiResponse<Page<PostResponseDto>> list(
            @RequestParam(required = false) String category,
            Pageable pageable
    ) {
        Page<PostResponseDto> posts = postService.getPosts(category, pageable);
        for (PostResponseDto post : posts) {
            ResponseEntity<ApiResponse<PublicUserDTO>> userInfo = userClient.getBasicInfo(post.getUserId());
            log.info("[userInfo] =======> {}", userInfo);
            post.setNickName(userInfo.getBody().getData().getNickname());
        }
        log.info("[list] posts=============================> {}", posts);
        return ApiResponse.success(posts);

    }

    private List<MultipartFile> collectFiles(MultipartHttpServletRequest request) {
        if (request == null) return Collections.emptyList();

        List<MultipartFile> result = new ArrayList<>();

        // 미리 정의한 파트 이름들 우선 수집
        for (String name : FILE_PART_NAMES) {
            List<MultipartFile> files = request.getFiles(name);
            if (!CollectionUtils.isEmpty(files)) {
                result.addAll(files.stream()
                        .filter(f -> f != null && !f.isEmpty())
                        .collect(Collectors.toList()));
            }
        }

        if (result.isEmpty()) {
            Map<String, MultipartFile> fileMap = request.getFileMap();
            if (!CollectionUtils.isEmpty(fileMap)) {
                for (MultipartFile f : fileMap.values()) {
                    if (f != null && !f.isEmpty()) {
                        result.add(f);
                    }
                }
            }
        }

        return result;
    }

    private String safeName(String name) {
        return StringUtils.hasText(name) ? name : "(no-name)";
    }
}
