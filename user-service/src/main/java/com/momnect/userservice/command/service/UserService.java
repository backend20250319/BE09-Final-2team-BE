package com.momnect.userservice.command.service;

import com.momnect.userservice.command.client.ProductClient;
import com.momnect.userservice.command.dto.*;
import com.momnect.userservice.command.entity.User;
import com.momnect.userservice.command.mapper.UserMapper;
import com.momnect.userservice.command.repository.UserRepository;
import com.momnect.userservice.exception.DuplicateUserException;
import com.momnect.userservice.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final ChildService childService;
    private final ProductClient productClient; // Feign Client 주입
    //private final ReviewServiceClient reviewServiceClient;   // Feign Client 주입

    /**
     * 마이페이지 대시보드 정보 조회
     */
    @Transactional(readOnly = true)
    public MypageDTO getMypageDashboard(Long userId) {
        // 1. 프로필 정보 조회
        PublicUserDTO profileInfo = getPublicUserProfile(userId);

        // 2. 자녀 정보 조회
        List<ChildDTO> children = childService.getChildren(userId); // 주입받은 인스턴스 사용

        // 3. 거래 현황 조회 (상품 서비스 연동)
        // Feign Client를 사용해 상품 서비스의 API 연동
        TransactionSummaryDTO transactionSummary = productClient
                .getMyTransactionSummary(userId)
                .getData();

        // 4. 리뷰 개수 조회 (리뷰 서비스 연동) (리뷰 서비스 연동 - TODO: API 구현 후 활성화)
//        int reviewCount = reviewServiceClient.getMyReviewCount(userId).getData();
//        transactionSummary.setReviewCount(reviewCount);

        // MypageDTO를 빌더 패턴으로 생성하여 반환
        return MypageDTO.builder()
                .profileInfo(profileInfo)
                .childList(children)
                .transactionSummary(transactionSummary)
                .build();
    }

    /**
     * 1. 프로필 수정 (닉네임, 이메일, 휴대폰번호만)
     */
    public UserDTO updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        if (user.getIsDeleted()) {
            throw new UserNotFoundException("탈퇴한 사용자입니다.");
        }

        // 닉네임 중복 검사 (본인 제외)
        if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
            if (userRepository.existsByNicknameAndIdNot(request.getNickname(), userId)) {
                throw new DuplicateUserException("이미 사용 중인 닉네임입니다.");
            }
        }

        // 이메일 중복 검사 (본인 제외)
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(request.getEmail(), userId)) {
                throw new DuplicateUserException("이미 사용 중인 이메일입니다.");
            }
        }

        // 프로필 업데이트
        if (request.getNickname() != null) user.setNickname(request.getNickname());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhoneNumber() != null) {
            String cleanPhoneNumber = request.getPhoneNumber().replaceAll("[^0-9]", "");
            user.setPhoneNumber(cleanPhoneNumber);
        }

        user.setUpdatedBy(userId);
        User savedUser = userRepository.save(user);

        return userMapper.toUserDTO(savedUser);
    }

    /**
     * 2. 비밀번호 변경
     */
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        if (user.getIsDeleted()) {
            throw new UserNotFoundException("탈퇴한 사용자입니다.");
        }

        // 소셜 로그인 사용자는 비밀번호 변경 불가
        if (!"LOCAL".equals(user.getOauthProvider())) {
            throw new RuntimeException("소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.");
        }

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호 설정
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedBy(userId);
        userRepository.save(user);
    }

    /**
     * 3. 회원탈퇴
     */
    public void deleteAccount(Long userId, DeleteAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        if (user.getIsDeleted()) {
            throw new UserNotFoundException("이미 탈퇴한 사용자입니다.");
        }

        // 비밀번호 확인 (소셜 로그인 사용자는 제외)
        if ("LOCAL".equals(user.getOauthProvider())) {
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new RuntimeException("비밀번호가 일치하지 않습니다.");
            }
        }

        // 탈퇴 처리
        user.setIsDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        user.setIsWithdrawalAgreed(request.getIsWithdrawalAgreed());
        user.setRefreshToken(null); // 토큰 제거
        user.setUpdatedBy(userId);

        userRepository.save(user);
    }

    /**
     * 4. 타 사용자 프로필 조회 (공개 정보만)
     */
    @Transactional(readOnly = true)
    public PublicUserDTO getPublicUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        if (user.getIsDeleted()) {
            throw new UserNotFoundException("탈퇴한 사용자입니다.");
        }

        return userMapper.toPublicUserDTO(user, false);
    }

    /**
     * 5. 중복 확인 (통합 API용)
     */
    public boolean checkDuplicate(String type, String value) {
        switch (type.toLowerCase()) {
            case "loginid":
                return userRepository.existsByLoginId(value);
            case "nickname":
                return userRepository.existsByNickname(value);
            case "email":
                return userRepository.existsByEmail(value);
            default:
                throw new IllegalArgumentException("지원하지 않는 타입입니다: " + type);
        }
    }

    /**
     * 6. 사용자 존재 여부 확인 (다른 서비스용)
     */
    @Transactional(readOnly = true)
    public boolean checkUserExists(Long userId) {
        return userRepository.findById(userId)
                .map(user -> !user.getIsDeleted())
                .orElse(false);
    }

    /**
     * 7. 프로필 수정용 상세 정보 조회
     */
    @Transactional(readOnly = true)
    public UserDTO getProfileForEdit(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        if (user.getIsDeleted()) {
            throw new UserNotFoundException("탈퇴한 사용자입니다.");
        }

        return userMapper.toUserDTO(user);
    }
}