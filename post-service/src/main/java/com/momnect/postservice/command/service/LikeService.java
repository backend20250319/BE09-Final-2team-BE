package com.momnect.postservice.command.service;

import com.momnect.postservice.command.dto.LikeResponse;
import com.momnect.postservice.command.dto.LikeSummaryResponse;
import com.momnect.postservice.command.entity.PostLike;
import com.momnect.postservice.command.entity.PostLikeId;
import com.momnect.postservice.command.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final PostLikeRepository likeRepository;

    @Transactional
    public LikeResponse like(Long postId, Long userId) {
        PostLikeId id = new PostLikeId(postId, userId);
        if (!likeRepository.existsById(id)) {
            likeRepository.save(PostLike.builder().id(id).build());
        }
        long cnt = likeRepository.countByIdPostId(postId);
        return LikeResponse.builder().liked(true).likeCount(cnt).build();
    }

    @Transactional
    public LikeResponse unlike(Long postId, Long userId) {
        PostLikeId id = new PostLikeId(postId, userId);
        if (likeRepository.existsById(id)) {
            likeRepository.deleteById(id);
        }
        long cnt = likeRepository.countByIdPostId(postId);
        return LikeResponse.builder().liked(false).likeCount(cnt).build();
    }

    @Transactional(readOnly = true)
    public LikeSummaryResponse summary(Long postId) {
        long cnt = likeRepository.countByIdPostId(postId);
        List<Long> userIds = likeRepository.findUserIdsByPostId(postId);
        return LikeSummaryResponse.builder()
                .likeCount(cnt)
                .userIds(userIds)
                .build();
    }
}
