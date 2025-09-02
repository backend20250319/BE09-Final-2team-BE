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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    public Long createPost(PostRequestDto dto, List<MultipartFile> images) {
        PostCategory category = requireCategoryByName(dto.getCategoryName());

        // 1) 파일 업로드 (기존 FileServiceClient 사용)
        List<ImageFileDTO> uploaded = (images == null || images.isEmpty())
                ? Collections.emptyList()
                : fileServiceClient.upload(images);

        uploaded.forEach(f -> log.info("[Upload] original={}, stored(guess)={}, path={}, url={}",
                f.getFilename(),                       // 업로더가 알려준 원본 파일명(있다면)
                lastSegment(pathFromUrl(f.getUrl())),  // URL에서 추정한 저장파일명
                f.getPath(), f.getUrl()));

        // 2) 이미지 메타 저장 (tbl_image_file) → path는 항상 "/폴더/저장파일명", stored_name은 항상 "저장파일명"
        List<Long> imageIds = new ArrayList<>();
        if (!uploaded.isEmpty()) {
            IntStream.range(0, uploaded.size()).forEach(i -> {
                ImageFileDTO u = uploaded.get(i);
                MultipartFile mf = images.get(i); // 같은 순서라고 가정

                Optional<FileEntity> saved = storeOrMirrorImage(u, mf);
                saved.ifPresent(fe -> imageIds.add(fe.getId()));
            });
        }

        Long coverId = imageIds.isEmpty() ? null : imageIds.get(0);
        boolean hasImage = !imageIds.isEmpty();

        // 3) 게시글 저장
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

        // 4) post_image 링크 저장
        if (hasImage) {
            for (Long fid : imageIds) {
                postImageRepository.save(new PostImage(post.getId(), fid));
            }
        }

        return post.getId();
    }

    /**
     * 업로더가 준 URL(또는 path)을 기준으로 최종 저장경로(finalPath)를 확정하고,
     * stored_name은 finalPath의 마지막 세그먼트로 '강제'한다.
     *
     * 최종 저장 규칙
     *  - URL이 있으면 URL의 path를 그대로 사용: "/{dir}/{storedName}"
     *  - URL이 없고 path(디렉토리)만 있으면: "/{dir}/" + "타임스템프_UUID.ext" 생성
     *  - DB에 저장:
     *      path        = finalPath ("/dir/파일명" 전체)
     *      stored_name = lastSegment(finalPath) (항상 저장파일명)
     *      original    = 업로드 원본명(조회용)
     */
    private Optional<FileEntity> storeOrMirrorImage(ImageFileDTO u, MultipartFile mf) {
        try {
            // 1) 업로더가 반환한 URL에서 경로를 우선 파싱 (예: "/2/1756...uuid.png")
            String urlPath = pathFromUrl(u.getUrl());        // null 가능
            String baseDir = normalizeDir(u.getPath());      // "2" -> "/2/"

            // 2) 최종 경로(finalPath) 결정
            //    - URL 경로가 있으면 그대로 사용 (서버에 실제로 올라간 파일명과 일치)
            //    - 없으면 baseDir + 랜덤파일명 으로 생성
            String finalPath;
            if (!isBlank(urlPath) && urlPath.contains("/")) {
                finalPath = ensureLeadingSlash(urlPath);     // 이미 "/dir/name"
            } else {
                String extHint = extensionOf(
                        (mf != null && mf.getOriginalFilename() != null) ? mf.getOriginalFilename()
                                : u.getFilename()
                );
                String random = randomName(extHint);         // 예: 1756698093555_uuid.png
                finalPath = baseDir + random;                // 예: "/2/1756...uuid.png"
            }

            // 3) stored_name을 finalPath의 마지막 세그먼트로 '강제'
            String storedName = lastSegment(finalPath);

            // 4) 나머지 메타데이터
            String originalName = (mf != null && mf.getOriginalFilename() != null)
                    ? mf.getOriginalFilename()
                    : firstNonBlank(u.getFilename(), storedName);

            String ext = extensionOf(storedName);
            long size = (u.getSize() != null) ? u.getSize() : (mf != null ? mf.getSize() : 0L);

            FileEntity fe = FileEntity.builder()
                    .originalName(originalName)           // 원본명(조회/표시용)
                    .storedName(storedName)               // ★ 실제 저장파일명(랜덤 보장)
                    .path(ensureLeadingSlash(finalPath))  // ★ "/폴더/저장파일명" 형태(완전체)
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
        return postRepository.findAll(pageable).map(PostResponseDto::new);
    }

    @Transactional
    public void updatePost(Long id, PostRequestDto dto) {
        Post origin = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));

        PostCategory newCategory = (dto.getCategoryName() != null)
                ? requireCategoryByName(dto.getCategoryName())
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

    // ===== 유틸 =====
    private static String firstNonBlank(String... xs) {
        if (xs == null) return null;
        for (String x : xs) if (!isBlank(x)) return x;
        return null;
    }
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static String pathFromUrl(String url) {
        if (isBlank(url)) return null;
        try {
            String p = URI.create(url).getPath(); // "/2/1756...uuid.png"
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
