package com.momnect.productservice.command.repository;

import com.momnect.productservice.command.document.ProductDocument;
import com.momnect.productservice.command.entity.product.RecommendedAge;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {

    // 공통 필터: 삭제 아님 + 판매완료 아님
    List<ProductDocument> findByIsDeletedFalseAndTradeStatusNot(String tradeStatus, Pageable pageable);

    // 연령대 추천
    List<ProductDocument> findByIsDeletedFalseAndTradeStatusNotAndRecommendedAge(
            String tradeStatus, RecommendedAge recommendedAge, Pageable pageable
    );

    // 기간 제한(선택): 최근 N일 필터가 필요할 때
    List<ProductDocument> findByIsDeletedFalseAndTradeStatusNotAndCreatedAtAfter(
            String tradeStatus, LocalDateTime after, Pageable pageable
    );
}
