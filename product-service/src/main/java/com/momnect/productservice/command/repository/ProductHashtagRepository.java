package com.momnect.productservice.command.repository;

import com.momnect.productservice.command.entity.hashtag.ProductHashtag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductHashtagRepository extends JpaRepository<ProductHashtag, Long> {
}
