package com.momnect.productservice.command.service;

import com.momnect.productservice.command.dto.ProductDocument;
import com.momnect.productservice.command.dto.ProductRequest;
import com.momnect.productservice.command.entity.*;
import com.momnect.productservice.command.repository.ProductCategoryRepository;
import com.momnect.productservice.command.repository.ProductRepository;
import com.momnect.productservice.command.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductSearchRepository searchRepository;

    @Transactional
    public Long createProduct(ProductRequest dto) {
        ProductCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid category ID"));

        Product product = Product.builder()
                .category(category)
                .sellerId(dto.getSellerId())
                .buyerId(dto.getBuyerId())
                .name(dto.getName())
                .content(dto.getContent())
                .price(dto.getPrice())
                .productStatus(ProductStatus.valueOf(dto.getProductStatus()))
                .tradeStatus(TradeStatus.valueOf(dto.getTradeStatus()))
                .recommendedAge(RecommendedAge.valueOf(dto.getRecommendedAge()))
                .viewCount(dto.getViewCount())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy(dto.getSellerId())
                .updatedBy(dto.getSellerId())
                .build();

        Product saved = productRepository.save(product);

        // Elasticsearch 색인
        indexProduct(saved);

        return saved.getId();
    }

    public void indexProduct(Product product) {
        ProductDocument doc = ProductDocument.builder()
                .id(product.getId())
                .categoryId(product.getCategory()
                        .getId())
                .sellerId(product.getSellerId())
                .name(product.getName())
                .content(product.getContent())
                .price(product.getPrice())
                .productStatus(product.getProductStatus()
                        .name())
                .tradeStatus(product.getTradeStatus()
                        .name())
                .recommendedAge(product.getRecommendedAge()
                        .name())
                .viewCount(product.getViewCount())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .soldAt(product.getSoldAt())
                .build();

        searchRepository.save(doc);
    }
}
