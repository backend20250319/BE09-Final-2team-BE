package com.momnect.postservice.command.service;

import com.momnect.postservice.command.dto.PostRequestDto;
import com.momnect.postservice.command.dto.PostResponseDto;
import com.momnect.postservice.command.entity.Post;
import com.momnect.postservice.command.entity.PostCategory;
import com.momnect.postservice.command.entity.PostEditorFile;
import com.momnect.postservice.command.repository.PostCategoryRepository;
import com.momnect.postservice.command.repository.PostEditorFileRepository;
import com.momnect.postservice.command.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostCategoryRepository categoryRepository;
    private final PostEditorFileRepository postEditorFileRepository;

    @Transactional
    public Long createPost(PostRequestDto dto, List<MultipartFile> images) {

        // ① 카테고리
        PostCategory category = categoryRepository.findByName(dto.getCategoryName());
        if (category == null) {
            category = categoryRepository.save(new PostCategory(dto.getCategoryName()));
        }

        // ② 게시글 저장
        Post post = Post.builder()
                .userId(dto.getUserId())
                .title(dto.getTitle())
                .contentHtml(dto.getContentHtml())
                .hasImage(images != null && !images.isEmpty())
                .category(category)
                .build();
        postRepository.save(post);

        // ③ 첨부파일 저장
        if (images != null && !images.isEmpty()) {
            for (MultipartFile file : images) {
                String originName = file.getOriginalFilename();
                String renamed = UUID.randomUUID() + "_" + originName;

                // (선택) 실제 폴더 저장
                // file.transferTo(new File("C:/upload/" + renamed));

                postEditorFileRepository.save(
                        PostEditorFile.builder()
                                .originalFileName(originName)
                                .renameFileName(renamed)
                                .post(post)
                                .build()
                );
            }
        }

        return post.getId();
    }


    @Transactional(readOnly = true)
    public PostResponseDto getPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post Not Found"));
        post.addViews();
        return new PostResponseDto(post);
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDto> getPosts(String categoryName, Pageable pageable) {
        return postRepository.findAll(pageable).map(PostResponseDto::new);
    }

    @Transactional
    public void updatePost(Long id, PostRequestDto dto) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post Not Found"));
        post.update(dto.getTitle(), dto.getContentHtml(), dto.getHasImage());
    }

    @Transactional
    public void deletePost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post Not Found"));
        post.softDelete();
    }
}
