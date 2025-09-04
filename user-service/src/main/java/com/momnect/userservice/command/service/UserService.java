package com.momnect.userservice.command.service;

import com.momnect.userservice.command.client.FileClient;
import com.momnect.userservice.command.client.ProductClient;
import com.momnect.userservice.command.client.ReviewClient;
import com.momnect.userservice.command.dto.auth.ChangePasswordRequest;
import com.momnect.userservice.command.dto.child.ChildDTO;
import com.momnect.userservice.command.dto.common.LocationSearchDTO;
import com.momnect.userservice.command.dto.common.ProductSummaryDTO;
import com.momnect.userservice.command.dto.common.TradeLocationDTO;
import com.momnect.userservice.command.dto.common.TransactionSummaryDTO;
import com.momnect.userservice.command.dto.mypage.MypageDTO;
import com.momnect.userservice.command.dto.mypage.OtherUserProfileDTO;
import com.momnect.userservice.command.dto.mypage.OtherUserTransactionSummaryDTO;
import com.momnect.userservice.command.dto.user.DeleteAccountRequest;
import com.momnect.userservice.command.dto.user.PublicUserDTO;
import com.momnect.userservice.command.dto.user.UpdateProfileRequest;
import com.momnect.userservice.command.dto.user.UserDTO;
import com.momnect.userservice.command.entity.User;
import com.momnect.userservice.command.mapper.UserMapper;
import com.momnect.userservice.command.repository.UserRepository;
import com.momnect.userservice.exception.DuplicateUserException;
import com.momnect.userservice.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final ChildService childService;
    private final ProductClient productClient;
    private final ReviewClient reviewClient;
    private final FileClient fileClient;

    @Value("${ftp.file-server-url}")
    private String fileServerUrl;

    private static final List<String>  DEFAULT_PROFILE_IMAGE_PATHS = List.of(
            "/2/1756892223966_3b405814-568e-4c17-9c8a-6eebff1429f0.png",
            "/2/1756892251934_5ede86b7-b37f-4550-81ea-4987bc81e2bd.png",
            "/2/1756892272610_bc3b5443-e7f9-42e8-a83b-b3d93e04b050.png",
            "/2/1756892288790_890b317f-320a-4a55-b590-f66f81dbe794.png"
    );

    /**
     * 마이페이지 대시보드 정보 조회
     */
    @Transactional(readOnly = true)
    public MypageDTO getMypageDashboard(Long userId) {
        log.info("마이페이지 대시보드 조회 시작 - userId: {}", userId);

        // 1. 프로필 정보 조회
        PublicUserDTO profileInfo = getPublicUserProfile(userId);

        // 2. 자녀 정보 조회
        List<ChildDTO> children = childService.getChildren(userId); // 주입받은 인스턴스 사용

        // 3. 거래 현황 조회 (상품 서비스 연동)
        // Feign Client를 사용해 상품 서비스의 API 연동
        TransactionSummaryDTO transactionSummary = productClient
                .getMyTransactionSummary()
                .getData();

        log.info("상품서비스에서 받은 거래현황: {}", transactionSummary);

        // 4. 리뷰 개수 조회 (리뷰 서비스 연동) (리뷰 서비스 연동 - TODO: API 구현 후 활성화)
        transactionSummary.setReviewCount(0); // 임시로 0 설정

        // MypageDTO를 빌더 패턴으로 생성하여 반환
        return MypageDTO.builder()
                .profileInfo(profileInfo)
                .childList(children)
                .transactionSummary(transactionSummary)
                .build();
    }

    /**
     * 지역 검색
     */
    @Transactional(readOnly = true)
    public List<LocationSearchDTO> searchAreas(String emd) {
        return productClient.searchAreas(emd).getData();
    }

    /**
     * 내 거래지역 수정
     */
    public void updateTradeLocations(Long userId, List<Long> areaIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        // 지역 ID 리스트를 콤마로 구분된 문자열로 변환
        String tradeAreaIds = areaIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        user.setTradeAreaIds(tradeAreaIds);
        userRepository.save(user);

        log.info("거래 지역 수정 완료 - userId: {}, areaIds: {}", userId, tradeAreaIds);
    }

    /**
     * 내 거래지역 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TradeLocationDTO> getMyTradeLocations(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        // 설정된 거래지역이 없는 경우 빈 리스트 반환
        if (user.getTradeAreaIds() == null || user.getTradeAreaIds().isEmpty()) {
            return Collections.emptyList();
        }

        // 상품 서비스에서 지역 ID들로 상세 정보 조회
        return productClient.getAreasByIds(user.getTradeAreaIds()).getData();
    }


    /**
     * 거래 현황 요약 정보 조회 (상품 서비스 연동)
     */
    @Transactional(readOnly = true)
    public TransactionSummaryDTO getTransactionSummary(Long userId) {
        TransactionSummaryDTO summary = productClient.getMyTransactionSummary().getData();

        // 마이페이지용이므로 임시로 0 설정
        summary.setReviewCount(0);

        // 리뷰 개수 연동 추가
//        try {
//            Integer reviewCount = reviewClient.getReviewCountByUserId(userId).getCount();
//            summary.setReviewCount(reviewCount);
//        } catch (Exception e) {
//            log.warn("리뷰 서비스 연동 실패 - userId: {}, error: {}", userId, e.getMessage());
//            summary.setReviewCount(0);
//        }

        return summary;
    }

    /**
     * 구매 상품 목록 조회 (상품 서비스 연동)
     */
    @Transactional(readOnly = true)
    public List<ProductSummaryDTO> getPurchasedProducts(Long userId) {
        return productClient.getMyPurchases().getData();
    }

    /**
     * 판매 상품 목록 조회 (상품 서비스 연동)
     */
    @Transactional(readOnly = true)
    public List<ProductSummaryDTO> getSoldProducts(Long userId) {
        return productClient.getMySales().getData();
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

        // 요청에 profileImageUrl이 명시적으로 포함된 경우에만 업데이트
        // 새로운 이미지가 업로드되면 URL이 들어올 것이고, 그렇지 않으면 null일 것
        // 여기서 null 체크를 통해 기존 값이 덮어쓰여지는 것을 방지
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
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

        // 프로필 이미지가 없는 경우 기본 이미지 URL을 설정
        String profileImageUrl = user.getProfileImageUrl();
        if (profileImageUrl == null || profileImageUrl.isEmpty()) {
            int index = (int) (userId %  DEFAULT_PROFILE_IMAGE_PATHS.size());
            profileImageUrl = fileServerUrl + DEFAULT_PROFILE_IMAGE_PATHS.get(index);
        }

        // 거래지역 조회
        List<String> tradeLocations = Collections.emptyList();
        String tradeAreaIds = user.getTradeAreaIds();

        if (tradeAreaIds != null && !tradeAreaIds.isEmpty()) {
            List<TradeLocationDTO> areas = productClient.getAreasByIds(tradeAreaIds).getData();
            tradeLocations = areas.stream()
                    .map(TradeLocationDTO::getEmd)
                    .toList();
        }

        return PublicUserDTO.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(profileImageUrl)
                .createdAt(user.getCreatedAt())
                .tradeLocations(tradeLocations)
                .build();
    }

    /**
     * 타 사용자 프로필 페이지 정보 조회 (통합)
     */
    @Transactional(readOnly = true)
    public OtherUserProfileDTO getOtherUserProfile(Long userId) {
        log.info("타사용자 프로필 조회 시작 - userId: {}", userId);

        // 1. 프로필 기본 정보 조회 (기존 메서드 재사용)
        PublicUserDTO profileInfo = getPublicUserProfile(userId);
        log.info("프로필 정보 조회 완료");

        // 2. 거래 현황 요약 정보 조회 (상품 클라이언트 연동)
        log.info("상품서비스 거래현황 조회 시작");
        TransactionSummaryDTO responseFromProductService = productClient.getOtherUserTransactionSummary(userId).getData();
        log.info("상품서비스에서 받은 타사용자 거래현황: {}", responseFromProductService);

        // 3. 판매 상품 목록 조회 (상품 클라이언트 연동)
        log.info("상품서비스 판매상품 조회 시작");
        List<ProductSummaryDTO> soldProducts = productClient.getOtherUserSales(userId).getData();
        log.info("상품서비스에서 받은 판매상품 개수: {}", soldProducts.size());

        // 4. 리뷰 개수 조회 (새로 추가)
        Integer reviewCount = 0;
        try {
            reviewCount = reviewClient.getReviewCountByUserId(userId).getCount();
        } catch (Exception e) {
            log.warn("리뷰 서비스 연동 실패 - userId: {}, error: {}", userId, e.getMessage());
        }

        OtherUserTransactionSummaryDTO otherUserSummary = OtherUserTransactionSummaryDTO.builder()
                .totalSalesCount(responseFromProductService.getTotalSalesCount()) // 전체 판매 완료 상품 개수
                .salesCount(responseFromProductService.getSalesCount()) // 현재 판매 중인 상품 개수
                .reviewCount(reviewCount) // 리뷰 개수 설정
                .build();

        // 4. 통합 DTO를 빌더 패턴으로 생성하여 반환
        return OtherUserProfileDTO.builder()
                .profileInfo(profileInfo)
                .transactionSummary(otherUserSummary)
                .soldProducts(soldProducts)
                .build();
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