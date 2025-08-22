package com.momnect.userservice.command.repository;

import com.momnect.userservice.command.entity.Child;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChildRepository extends JpaRepository<Child, Long> {

    // 특정 사용자의 자녀 목록 조회 (등록 순서대로)
    List<Child> findByUserIdOrderByIdAsc(Long userId);

    // 특정 사용자의 특정 자녀 조회 (본인 자녀만 접근 가능)
    Optional<Child> findByIdAndUserId(Long id, Long userId);

    // 특정 사용자의 자녀 수 조회 (최대 2명 제한 체크용)
    int countByUserId(Long userId);

    // 특정 사용자의 자녀 존재 여부 확인
    boolean existsByUserId(Long userId);
}