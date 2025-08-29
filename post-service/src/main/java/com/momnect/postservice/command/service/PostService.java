package com.momnect.postservice.command.service;

import com.momnect.postservice.command.client.FileServiceClient;
import com.momnect.postservice.command.dto.ImageFileDTO;
import com.momnect.postservice.command.dto.PostRequestDto;
import com.momnect.postservice.command.dto.PostResponseDto;
import com.momnect.postservice.command.entity.Post;
import com.momnect.postservice.command.entity.PostCategory;
import com.momnect.postservice.command.repository.PostCategoryRepository;
import com.momnect.postservice.command.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostCategoryRepository postCategoryRepository;
    private final ContentUploadProcessor contentUploadProcessor;  // 본문 내 dataURL → 파일서버 업로드 → URL 치환
    private final FileServiceClient fileServiceClient;            // 추가 첨부(images) 업로드

    /**
     * 1) contentHtml의 data:image/*를 파일서버에 업로드하고 public URL로 치환
     * 2) Post 저장
     * 3) images 추가 첨부 업로드 (메타 저장은 현재 생략)
     */
    @Transactional
    public Long createPost(PostRequestDto dto, List<MultipartFile> images) {
        // 카테고리 필수
        if (dto.getCategoryName() == null || dto.getCategoryName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "필수값 누락: categoryName");
        }

        // 카테고리 조회 (동명이 여러 개면 가장 먼저 만든 것 사용)
        PostCategory category = postCategoryRepository
                .findTopByNameOrderByIdAsc(dto.getCategoryName())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 카테고리: " + dto.getCategoryName()));

        // 1) 본문 내 base64 → 파일서버 업로드 → URL 치환
        var processed = contentUploadProcessor.processHtml(dto.getContentHtml());
        dto.setContentHtml(processed.finalHtml());

        boolean hasReqImages = images != null && !images.isEmpty();
        boolean hasImage = (dto.getHasImage() != null && dto.getHasImage()) || hasReqImages;

        // 2) 엔티티 저장 (필드명은 프로젝트 엔티티에 맞게 조정)
        Post post = Post.builder()
                .title(dto.getTitle())
                .contentHtml(dto.getContentHtml())
                .category(category)
                .userId(dto.getUserId())
                .hasImage(hasImage)
                .viewCount(0)
                .build();

        postRepository.save(post);

        // 3) images 업로드 (게시글 저장과 분리: 실패해도 게시글은 남김)
        if (hasReqImages) {
            try {
                List<ImageFileDTO> uploaded = fileServiceClient.upload(images);
                log.info("첨부 업로드 성공 postId={}, count={}", post.getId(), uploaded.size());
            } catch (Exception e) {
                log.warn("첨부 업로드 실패(게시글은 저장됨) postId={}, err={}", post.getId(), e.toString());
            }
        }

        return post.getId();
    }

    /** ✅ 단건 조회 */
    @Transactional(readOnly = true)
    public PostResponseDto getPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글 없음: " + id));
        return new PostResponseDto(post);
    }

    /** ✅ 목록 조회 (카테고리 선택적) */
    @Transactional(readOnly = true)
    public Page<PostResponseDto> getPosts(String categoryName, Pageable pageable) {
        Page<Post> page;
        if (categoryName == null || categoryName.isBlank()) {
            page = postRepository.findAll(pageable);
        } else {
            PostCategory category = postCategoryRepository
                    .findTopByNameOrderByIdAsc(categoryName)
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 카테고리: " + categoryName));
            page = postRepository.findByCategory_Id(category.getId(), pageable);
        }
        return page.map(PostResponseDto::new);
    }

    @Transactional
    public void updatePost(Long id, PostRequestDto dto) {
        throw new UnsupportedOperationException("Use your existing implementation.");
    }

    @Transactional
    public void deletePost(Long id) {
        throw new UnsupportedOperationException("Use your existing implementation.");
    }
}
