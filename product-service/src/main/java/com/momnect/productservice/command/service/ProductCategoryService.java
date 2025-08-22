package com.momnect.productservice.command.service;

import com.momnect.productservice.command.dto.product.ProductCategoryDto;
import com.momnect.productservice.command.entity.product.ProductCategory;
import com.momnect.productservice.command.repository.ProductCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductCategoryService {
    @Value("classpath:data.sql")
    private Resource dataSql;

    private final JdbcTemplate jdbcTemplate;
    private final ProductCategoryRepository categoryRepository;

    /**
     * 상품 카테고리 전체 트리 조회
     *
     * @return 트리 구조의 카테고리 리스트
     */
    public List<ProductCategoryDto> getCategoryTree() {
        List<ProductCategory> roots = categoryRepository.findByParentIsNull();
        return roots.stream()
                .map(ProductCategoryDto::fromEntity)
                .collect(Collectors.toList());
    }

    /***
     * 카테고리 데이터 초기화
     */
    public void insertMockCategories() {
        try {
            String sql = new String(dataSql.getInputStream()
                    .readAllBytes());
            for (String statement : sql.split(";")) {
                if (!statement.trim()
                        .isEmpty()) {
                    jdbcTemplate.execute(statement);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("data.sql 실행 중 오류 발생", e);
        }
    }
}
