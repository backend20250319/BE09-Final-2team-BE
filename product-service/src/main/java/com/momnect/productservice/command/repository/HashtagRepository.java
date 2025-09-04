package com.momnect.productservice.command.repository;

import com.momnect.productservice.command.entity.hashtag.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {
    Optional<Hashtag> findByName(String tagName);
}
