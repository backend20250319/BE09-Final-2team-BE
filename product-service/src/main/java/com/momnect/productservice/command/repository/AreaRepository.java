package com.momnect.productservice.command.repository;

import com.momnect.productservice.command.entity.Area;
import com.momnect.productservice.command.entity.AreaLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AreaRepository extends JpaRepository<Area, Integer> {

    // 이름으로 EMD 단위 검색
    List<Area> findByNameContainingAndLevel(String name, AreaLevel level);

    // 지역 코드 검색
    Optional<Area> findByCode(String code);
}
