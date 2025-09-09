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
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

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

    /** ---------- 생성 (멀티파트) ---------- */
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ApiResponse<Long> create(
            @AuthenticationPrincipal String userId,
            @RequestParam("title") String title,
            @RequestParam("contentHtml") String contentHtml,
            @RequestParam("categoryName") String categoryName,
            MultipartHttpServletRequest multipartRequest
    ) {
        log.info("[create] userId={}", userId);

        List<MultipartFile> images = collectFiles(multipartRequest);
        if (CollectionUtils.isEmpty(images)) {
            log.info("[create] images=empty");
        } else {
            log.info("[create] images size={}", images.size());
            for (MultipartFile f : images) {
                log.info(" - fn={}, size={}, type={}",
                        safeName(f.getOriginalFilename()), f.getSize(), f.getContentType());
            }
        }

        PostRequestDto dto = new PostRequestDto();
        dto.setUserId(Long.valueOf(userId));
        dto.setTitle(title);
        dto.setContentHtml(contentHtml);
        dto.setCategoryName(normalizeCategory(categoryName));
        dto.setHasImage(!CollectionUtils.isEmpty(images));

        Long id = postService.createPost(dto, images);
        return ApiResponse.success(id);
    }

    /** ---------- 단건 조회 ---------- */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getOne(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        PostResponseDto post = postService.getPost(id);

        // 작성자 닉네임 주입 (토큰 없는 경우는 생략 가능)
        if (StringUtils.hasText(authorization)) {
            ResponseEntity<ApiResponse<PublicUserDTO>> userInfo =
                    userClient.getBasicInfo(post.getUserId(), authorization);
            post.setNickName(userInfo.getBody().getData().getNickname());
        }

        List<CommentDtos.Response> comments = commentService.listForPost(id);
        LikeSummaryResponse likeSummary = likeService.summary(id);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("post", post);
        payload.put("comments", comments);
        payload.put("like", likeSummary);
        return ApiResponse.success(payload);
    }

    /** ---------- 목록 조회 (페이지네이션) ---------- */
    @GetMapping
    public ApiResponse<Page<PostResponseDto>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            Pageable pageable,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Sort sortSpec = resolveSort(sort, pageable);
        Pageable effective = resolvePageable(page, size, sortSpec, pageable);
        String normalizedCategory = normalizeCategory(category);

        Page<PostResponseDto> posts = postService.getPosts(normalizedCategory, effective);

        if (StringUtils.hasText(authorization)) {
            posts.forEach(p -> {
                ResponseEntity<ApiResponse<PublicUserDTO>> userInfo =
                        userClient.getBasicInfo(p.getUserId(), authorization);
                p.setNickName(userInfo.getBody().getData().getNickname());
            });
        }
        return ApiResponse.success(posts);
    }

    /** ---------- 소프트 삭제 ---------- */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal String userId
    ) {
        postService.deletePost(id);
        return ApiResponse.success(null);
    }


    private String normalizeCategory(String category) {
        if (!StringUtils.hasText(category)) return null;
        String c = category.trim();
        String lower = c.toLowerCase(Locale.ROOT);
        if (lower.equals("tips") || lower.equals("tip") || c.equals("육아 꿀팁")) return "Tip";
        if (lower.equals("auction") || c.equals("경매")) return "Auction";
        return c;
    }

    private Sort resolveSort(String sortParam, Pageable pageable) {
        Sort sortSpec;
        if (StringUtils.hasText(sortParam)) {
            List<Sort.Order> orders = new ArrayList<>();
            String[] parts = sortParam.split(";");
            for (String part : parts) {
                String p = part.trim();
                if (!StringUtils.hasText(p)) continue;
                String[] kv = p.split(",");
                String prop = kv[0].trim();
                Sort.Direction dir = (kv.length > 1 && "asc".equalsIgnoreCase(kv[1].trim()))
                        ? Sort.Direction.ASC : Sort.Direction.DESC;
                orders.add(new Sort.Order(dir, prop));
            }
            sortSpec = orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
        } else {
            sortSpec = (pageable != null && pageable.getSort() != null) ? pageable.getSort() : Sort.unsorted();
        }
        if (sortSpec == null || sortSpec.isUnsorted()) {
            sortSpec = Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return sortSpec;
    }

    private Pageable resolvePageable(Integer pageParam, Integer sizeParam, Sort sortSpec, Pageable pageable) {
        int pageIndex;
        int pageSize;

        if (pageParam != null && pageParam >= 0) {
            pageIndex = pageParam;
        } else if (pageable != null) {
            pageIndex = pageable.getPageNumber();
        } else {
            pageIndex = 0;
        }

        if (sizeParam != null && sizeParam > 0) {
            pageSize = sizeParam;
        } else if (pageable != null && pageable.getPageSize() > 0) {
            pageSize = pageable.getPageSize();
        } else {
            pageSize = 10;
        }
        return PageRequest.of(pageIndex, pageSize, sortSpec);
    }

    private List<MultipartFile> collectFiles(MultipartHttpServletRequest request) {
        if (request == null) return Collections.emptyList();

        List<MultipartFile> result = new ArrayList<>();

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

    private static String nullSafeTrim(Object v) {
        return v == null ? null : String.valueOf(v).trim();
    }
    private static Object firstNonNull(Object a, Object b) { return a != null ? a : b; }
}
