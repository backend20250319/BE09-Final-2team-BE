package com.momnect.postservice.command.service;

import com.momnect.postservice.command.repository.PostImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostQueryService {

    private final PostImageRepository postImageRepository;

    @Value("${file.public-base-url}")
    private String fileBaseUrl;

    public List<String> getPostImageUrls(Long postId) {
        List<String> paths = postImageRepository.findImagePathsByPostId(postId);
        String base = normalize(fileBaseUrl);
        return paths.stream()
                .map(p -> base + ensureLeadingSlash(p))
                .toList();
    }

    private String normalize(String base) {
        return (base == null || base.isBlank()) ? "" :
                (base.endsWith("/") ? base.substring(0, base.length()-1) : base);
    }
    private String ensureLeadingSlash(String p) {
        if (p == null || p.isBlank()) return "";
        return p.startsWith("/") ? p : "/" + p;
    }
}
