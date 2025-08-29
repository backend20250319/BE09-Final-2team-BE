package com.momnect.userservice.command.service;

import com.momnect.userservice.command.dto.child.ChildDTO;
import com.momnect.userservice.command.dto.child.CreateChildRequest;
import com.momnect.userservice.command.dto.child.UpdateChildRequest;
import com.momnect.userservice.command.entity.Child;
import com.momnect.userservice.command.repository.ChildRepository;
import com.momnect.userservice.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChildService {

    private final ChildRepository childRepository;
    private static final int MAX_CHILDREN = 2; // 최대 자녀 수 제한

    /**
     * 자녀 등록
     */
    public ChildDTO createChild(Long userId, CreateChildRequest request) {
        log.info("자녀 등록 시작: userId={}, nickname={}", userId, request.getNickname());

        // 입력값 검증
        validateCreateChildRequest(request);

        // 자녀 수 제한 확인
        validateChildrenLimit(userId);

        // 자녀 생성
        Child child = buildChildFromRequest(userId, request);
        Child savedChild = childRepository.save(child);

        log.info("자녀 등록 완료: childId={}", savedChild.getId());
        return toChildDTO(savedChild);
    }

    /**
     * 자녀 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ChildDTO> getChildren(Long userId) {
        log.info("자녀 목록 조회: userId={}", userId);

        List<Child> children = childRepository.findByUserIdOrderByIdAsc(userId);

        return children.stream()
                .map(this::toChildDTO)
                .collect(Collectors.toList());
    }

    /**
     * 자녀 정보 수정
     */
    public ChildDTO updateChild(Long userId, Long childId, UpdateChildRequest request) {
        log.info("자녀 수정 시작: userId={}, childId={}", userId, childId);

        // 자녀 조회 (본인 자녀만)
        Child child = childRepository.findByIdAndUserId(childId, userId)
                .orElseThrow(() -> new UserNotFoundException("자녀 정보를 찾을 수 없습니다"));

        // 입력값 검증
        validateUpdateChildRequest(request);

        // 자녀 정보 업데이트
        updateChildInfo(child, request);
        Child savedChild = childRepository.save(child);

        log.info("자녀 수정 완료: childId={}", childId);
        return toChildDTO(savedChild);
    }

    /**
     * 자녀 삭제
     */
    public void deleteChild(Long userId, Long childId) {
        log.info("자녀 삭제 시작: userId={}, childId={}", userId, childId);

        // 자녀 조회 (본인 자녀만)
        Child child = childRepository.findByIdAndUserId(childId, userId)
                .orElseThrow(() -> new UserNotFoundException("자녀 정보를 찾을 수 없습니다"));

        childRepository.delete(child);

        log.info("자녀 삭제 완료: childId={}", childId);
    }

    /**
     * 자녀 등록 요청 검증
     */
    private void validateCreateChildRequest(CreateChildRequest request) {
        if (request.getNickname() == null || request.getNickname().trim().isEmpty()) {
            throw new IllegalArgumentException("자녀 애칭을 입력해주세요");
        }

        if (request.getBirthDate() == null) {
            throw new IllegalArgumentException("생년월일을 입력해주세요");
        }

        if (request.getBirthDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("생년월일은 현재 날짜 이전이어야 합니다");
        }

        // 8세 미만만 등록 가능 (영유아 대상 서비스)
        if (request.getBirthDate().isBefore(LocalDate.now().minusYears(8))) {
            throw new IllegalArgumentException("8세 이상은 등록할 수 없습니다 (영유아 대상 서비스)");
        }
    }

    /**
     * 자녀 수정 요청 검증
     */
    private void validateUpdateChildRequest(UpdateChildRequest request) {
        if (request.getNickname() != null && request.getNickname().trim().isEmpty()) {
            throw new IllegalArgumentException("자녀 애칭은 비워둘 수 없습니다");
        }

        if (request.getBirthDate() != null) {
            if (request.getBirthDate().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("생년월일은 현재 날짜 이전이어야 합니다");
            }

            if (request.getBirthDate().isBefore(LocalDate.now().minusYears(8))) {
                throw new IllegalArgumentException("8세 이상은 등록할 수 없습니다 (영유아 대상 서비스)");
            }
        }
    }

    /**
     * 자녀 수 제한 확인
     */
    private void validateChildrenLimit(Long userId) {
        int currentChildrenCount = childRepository.countByUserId(userId);
        if (currentChildrenCount >= MAX_CHILDREN) {
            throw new IllegalArgumentException("최대 " + MAX_CHILDREN + "명까지만 등록 가능합니다");
        }
    }

    /**
     * CreateChildRequest에서 Child 엔티티 생성
     */
    private Child buildChildFromRequest(Long userId, CreateChildRequest request) {
        return Child.builder()
                .userId(userId)
                .nickname(request.getNickname().trim())
                .birthDate(request.getBirthDate())
                .build();
    }

    /**
     * 자녀 정보 업데이트
     */
    private void updateChildInfo(Child child, UpdateChildRequest request) {
        if (request.getNickname() != null) {
            child.setNickname(request.getNickname().trim());
        }
        if (request.getBirthDate() != null) {
            child.setBirthDate(request.getBirthDate());
        }
    }

    /**
     * Child 엔티티를 ChildDTO로 변환
     */
    private ChildDTO toChildDTO(Child child) {
        int age = calculateAge(child.getBirthDate());

        return ChildDTO.builder()
                .id(child.getId())
                .userId(child.getUserId())
                .nickname(child.getNickname())
                .birthDate(child.getBirthDate())
                .age(age)
                .build();
    }

    /**
     * 나이 계산 (년 단위)
     */
    private int calculateAge(LocalDate birthDate) {
        if (birthDate == null) return 0;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
