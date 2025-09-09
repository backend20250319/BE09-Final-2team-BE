package com.momnect.postservice.command.service;

import com.momnect.postservice.command.client.FileServiceClient;
import com.momnect.postservice.command.dto.ImageFileDTO;
import com.momnect.postservice.command.dto.PostRequestDto;
import com.momnect.postservice.command.dto.PostResponseDto;
import com.momnect.postservice.command.entity.FileEntity;
import com.momnect.postservice.command.entity.Post;
import com.momnect.postservice.command.entity.PostCategory;
import com.momnect.postservice.command.entity.PostImage;
import com.momnect.postservice.command.repository.FileEntityRepository;
import com.momnect.postservice.command.repository.PostCategoryRepository;
import com.momnect.postservice.command.repository.PostImageRepository;
import com.momnect.postservice.command.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.*;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostCategoryRepository postCategoryRepository;
    private final FileServiceClient fileServiceClient;
    private final PostImageRepository postImageRepository;
    private final FileEntityRepository fileEntityRepository;
    private final PostQueryService postQueryService;

    /* 카테고리 이름으로 조회 */
    private PostCategory requireCategoryByName(String name) {
        return postCategoryRepository.findAll().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다: " + name));
    }

    private String normalizeCategoryName(String category) {
        if (category == null) return null;
        String s = category.trim().toLowerCase();
        if (s.equals("tips") || s.equals("육아 꿀팁") || s.equals("tip")) return "Tip";
        if (s.equals("auction") || s.equals("경매")) return "Auction";
        return category;
    }

    @Transactional
    public Long createPost(PostRequestDto dto, List<MultipartFile> images) {
        String normalized = normalizeCategoryName(dto.getCategoryName());
        PostCategory category = requireCategoryByName(normalized);

        List<ImageFileDTO> uploaded = (images == null || images.isEmpty())
                ? Collections.emptyList()
                : fileServiceClient.upload(images);

        uploaded.forEach(f -> log.info("[Upload] original={}, stored(guess)={}, path={}, url={}",
                f.getFilename(),
                lastSegment(pathFromUrl(f.getUrl())),
                f.getPath(), f.getUrl()));

        List<Long> imageIds = new ArrayList<>();
        if (!uploaded.isEmpty()) {
            IntStream.range(0, uploaded.size()).forEach(i -> {
                ImageFileDTO u = uploaded.get(i);
                MultipartFile mf = images.get(i);
                storeOrMirrorImage(u, mf).ifPresent(fe -> imageIds.add(fe.getId()));
            });
        }

        Long coverId = imageIds.isEmpty() ? null : imageIds.get(0);
        boolean hasImage = !imageIds.isEmpty();

        Post post = Post.builder()
                .category(category)
                .title(dto.getTitle())
                .contentHtml(dto.getContentHtml())
                .userId(dto.getUserId())
                .viewCount(0)
                .isDeleted(false)
                .hasImage(hasImage)
                .coverFileId(coverId)
                .build();
        postRepository.save(post);

        if (hasImage) {
            for (Long fid : imageIds) {
                postImageRepository.save(new PostImage(post.getId(), fid));
            }
        }
        return post.getId();
    }

    private Optional<FileEntity> storeOrMirrorImage(ImageFileDTO u, MultipartFile mf) {
        try {
            String urlPath = pathFromUrl(u.getUrl());
            String baseDir = normalizeDir(u.getPath());

            String finalPath;
            if (!isBlank(urlPath) && urlPath.contains("/")) {
                finalPath = ensureLeadingSlash(urlPath);
            } else {
                String extHint = extensionOf(
                        (mf != null && mf.getOriginalFilename() != null) ? mf.getOriginalFilename() : u.getFilename()
                );
                finalPath = baseDir + randomName(extHint);
            }

            String storedName = lastSegment(finalPath);
            String originalName = (mf != null && mf.getOriginalFilename() != null)
                    ? mf.getOriginalFilename()
                    : firstNonBlank(u.getFilename(), storedName);

            String ext = extensionOf(storedName);
            long size = (u.getSize() != null) ? u.getSize() : (mf != null ? mf.getSize() : 0L);

            FileEntity fe = FileEntity.builder()
                    .originalName(originalName)
                    .storedName(storedName)
                    .path(ensureLeadingSlash(finalPath))
                    .size(size)
                    .extension(ext)
                    .isDeleted(false)
                    .build();

            FileEntity saved = (u.getId() != null)
                    ? fileEntityRepository.findById(u.getId()).orElseGet(() -> fileEntityRepository.save(fe))
                    : fileEntityRepository.save(fe);

            log.info("[ImageMeta] saved: id={}, originalName={}, storedName={}, path={}",
                    saved.getId(), saved.getOriginalName(), saved.getStoredName(), saved.getPath());

            return Optional.of(saved);

        } catch (Exception e) {
            log.warn("이미지 메타 저장 실패. url={}, path={}, err={}", u.getUrl(), u.getPath(), e.toString());
            return Optional.empty();
        }
    }

    @Transactional(readOnly = true)
    public PostResponseDto getPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));

        List<String> urls = post.isHasImage()
                ? postQueryService.getPostImageUrls(id)
                : List.of();

        PostResponseDto dto = new PostResponseDto(post);
        dto.setImageUrls(urls);
        return dto;
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDto> getPosts(String category, Pageable pageable) {
        Pageable effective = pageable;
        if (pageable.getSort() == null || pageable.getSort().isUnsorted()) {
            effective = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );
        }

        String normalized = normalizeCategoryName(category);

        Page<Post> page;
        if (normalized == null || normalized.isBlank()) {
            page = postRepository.findByIsDeletedFalse(effective);
        } else {
            page = postRepository.findByCategory_NameAndIsDeletedFalse(normalized, effective);
        }

        return page.map(PostResponseDto::new);
    }

    @Transactional
    public void updatePost(Long id, PostRequestDto dto) {
        Post origin = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));

        PostCategory newCategory = (dto.getCategoryName() != null)
                ? requireCategoryByName(normalizeCategoryName(dto.getCategoryName()))
                : origin.getCategory();

        Post changed = Post.builder()
                .id(origin.getId())
                .category(newCategory)
                .title(dto.getTitle() != null ? dto.getTitle() : origin.getTitle())
                .contentHtml(dto.getContentHtml() != null ? dto.getContentHtml() : origin.getContentHtml())
                .userId(origin.getUserId())
                .viewCount(origin.getViewCount())
                .isDeleted(origin.isDeleted())
                .hasImage(origin.isHasImage())
                .coverFileId(origin.getCoverFileId())
                .build();

        postRepository.save(changed);
    }

    @Transactional
    public void deletePost(Long id) {
        Post origin = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));

        if (origin.isDeleted()) return;

        Post deleted = Post.builder()
                .id(origin.getId())
                .category(origin.getCategory())
                .title(origin.getTitle())
                .contentHtml(origin.getContentHtml())
                .userId(origin.getUserId())
                .viewCount(origin.getViewCount())
                .isDeleted(true)
                .hasImage(origin.isHasImage())
                .coverFileId(origin.getCoverFileId())
                .build();

        postRepository.save(deleted);
    }


    private static String firstNonBlank(String... xs) {
        if (xs == null) return null;
        for (String x : xs) if (!isBlank(x)) return x;
        return null;
    }
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static String pathFromUrl(String url) {
        if (isBlank(url)) return null;
        try {
            String p = URI.create(url).getPath();
            return (p == null) ? null : p;
        } catch (Exception e) {
            return null;
        }
    }

    private static String lastSegment(String path) {
        if (isBlank(path)) return "";
        int idx = path.lastIndexOf('/');
        return (idx >= 0 && idx < path.length() - 1) ? path.substring(idx + 1) : path;
    }

    private static String extensionOf(String name) {
        int dot = (name == null) ? -1 : name.lastIndexOf('.');
        return (dot >= 0 && dot < name.length() - 1) ? name.substring(dot + 1) : "";
    }

    private static String ensureLeadingSlash(String p) {
        if (p == null || p.isEmpty()) return "/";
        return p.startsWith("/") ? p : "/" + p;
    }

    private static String normalizeDir(String p) {
        if (isBlank(p)) return "/";
        String s = p.replace("\\", "/");
        if (!s.startsWith("/")) s = "/" + s;
        if (!s.endsWith("/")) s = s + "/";
        return s;
    }

    private static String randomName(String ext) {
        String e = isBlank(ext) ? "" : (ext.startsWith(".") ? ext : "." + ext);
        return System.currentTimeMillis() + "_" + UUID.randomUUID() + e;
    }
}
