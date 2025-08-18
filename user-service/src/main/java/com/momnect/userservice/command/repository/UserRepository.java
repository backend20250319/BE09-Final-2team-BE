package com.momnect.userservice.command.repository;

import java.util.Optional;

import com.momnect.userservice.command.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    Optional<User> findByRefreshToken(String refreshToken);

    // 이메일 중복 체크용
    Optional<User> findByEmail(String email);

    // OAuth 사용자 찾기용 (향후 카카오 로그인 확장시)
    Optional<User> findByOauthProviderAndOauthId(String oauthProvider, String oauthId);
}
