package com.momnect.userservice.command.repository;

import java.util.Optional;

import com.momnect.userservice.command.dto.user.PublicUserDTO;
import com.momnect.userservice.command.entity.User;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 기본 조회 메서드들
    Optional<User> findByLoginId(String loginId);
    Optional<User> findByRefreshToken(String refreshToken);
    Optional<User> findByEmail(String email);
    // OAuth 사용자 찾기용 (향후 카카오 로그인 확장시)
    Optional<User> findByOauthProviderAndOauthId(String oauthProvider, String oauthId);

    // 증복 검사 메서드들 (본인 제외)
    boolean existsByNicknameAndIdNot(String nickname, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);

    // 일반 중복 검사 메서드들
    boolean existsByLoginId(String loginId);
    boolean existsByNickname(String nickname);
    boolean existsByEmail(String email);
}
