package com.momnect.userservice.command.service;

import com.momnect.userservice.command.dto.LoginResponse;
import com.momnect.userservice.command.dto.SignupRequest;
import com.momnect.userservice.command.dto.UserDTO;
import com.momnect.userservice.command.entity.User;
import com.momnect.userservice.command.repository.UserRepository;
import com.momnect.userservice.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    /**
     * 로그인: loginId + password 검증 후 AccessToken, RefreshToken 발급
     */
    public LoginResponse login(String loginId, String password) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        // UserDTO 직접 생성
        UserDTO userDTO = UserDTO.builder()
                .id(user.getId())
                .loginId(user.getLoginId())
                .name(user.getName())
                .role(user.getRole())
                .oauthProvider(user.getOauthProvider())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .address(user.getAddress())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deletedAt(user.getDeletedAt())
                .isDeleted(user.getIsDeleted())
                .deletionReason(user.getDeletionReason())
                .isTermsAgreed(user.getIsTermsAgreed())
                .isPrivacyAgreed(user.getIsPrivacyAgreed())
                .isWithdrawalAgreed(user.getIsWithdrawalAgreed())
                .createdBy(user.getCreatedBy())
                .updatedBy(user.getUpdatedBy())
                .build();

        return new LoginResponse(accessToken, refreshToken, userDTO);
    }

    /**
     * RefreshToken 기반 AccessToken 재발급
     */
    public String reissueAccessToken(String refreshToken) {
        // DB에서 해당 RefreshToken 존재 여부 확인
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid Refresh Token"));

        return jwtTokenProvider.reissueAccessToken(refreshToken);
    }

    /**
     * 회원가입
     */
    public LoginResponse signup(SignupRequest request) {
        // 중복 loginId 체크
        if (userRepository.findByLoginId(request.getLoginId()).isPresent()) {
            throw new RuntimeException("LoginId already exists");
        }

        // User 엔티티 생성 및 저장
        User user = User.builder()
                .loginId(request.getLoginId())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(request.getRole())
                .oauthProvider(request.getOauthProvider())
                .build();

        userRepository.save(user);

        // 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);

        // RefreshToken 저장
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        // UserDTO 직접 생성
        UserDTO userDTO = UserDTO.builder()
                .id(user.getId())
                .loginId(user.getLoginId())
                .name(user.getName())
                .role(user.getRole())
                .oauthProvider(user.getOauthProvider())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .address(user.getAddress())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deletedAt(user.getDeletedAt())
                .isDeleted(user.getIsDeleted())
                .deletionReason(user.getDeletionReason())
                .isTermsAgreed(user.getIsTermsAgreed())
                .isPrivacyAgreed(user.getIsPrivacyAgreed())
                .isWithdrawalAgreed(user.getIsWithdrawalAgreed())
                .createdBy(user.getCreatedBy())
                .updatedBy(user.getUpdatedBy())
                .build();

        return new LoginResponse(accessToken, refreshToken, userDTO);
    }

}
