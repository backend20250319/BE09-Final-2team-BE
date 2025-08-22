package com.momnect.productservice.command.repository;

import com.momnect.productservice.command.dto.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {
    // 기본 CRUD + 간단한 검색 메서드 정의 가능
}
