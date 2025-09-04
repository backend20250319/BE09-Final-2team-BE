package com.momnect.productservice.command.controller;

import com.momnect.productservice.command.dto.product.ProductCategoryDto;
import com.momnect.productservice.command.service.ProductCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("categories")
@RequiredArgsConstructor
public class CategoryController {

    private final ProductCategoryService categoryService;

    /**
     * 상품 카테고리 전체 트리 조회
     *
     * @return 트리 구조의 카테고리 리스트
     */
    @GetMapping("/tree")
    public ResponseEntity<List<ProductCategoryDto>> getCategoryTree() {
        return ResponseEntity.ok(categoryService.getCategoryTree());
    }

    /***
     * 카테고리 데이터 초기화
     *
     * @return String
     */
    @GetMapping("/init")
    public String initCategories() {
        categoryService.insertMockCategories();
        return "카테고리 데이터가 저장되었습니다.";
    }
}
