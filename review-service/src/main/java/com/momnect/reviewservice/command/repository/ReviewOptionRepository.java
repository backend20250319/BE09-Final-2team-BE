package com.momnect.reviewservice.command.repository;

import com.momnect.reviewservice.command.entity.ReviewOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewOptionRepository extends JpaRepository<ReviewOption, Long> {
    Optional<ReviewOption> findByName(String name);
}