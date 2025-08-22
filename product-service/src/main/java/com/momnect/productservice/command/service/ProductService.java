package com.momnect.productservice.command.service;

import com.momnect.productservice.command.dto.ProductDocument;
import com.momnect.productservice.command.dto.ProductRequest;
import com.momnect.productservice.command.entity.*;
import com.momnect.productservice.command.repository.ProductCategoryRepository;
import com.momnect.productservice.command.repository.ProductImageRepository;
import com.momnect.productservice.command.repository.ProductRepository;
import com.momnect.productservice.command.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductSearchRepository searchRepository;

    /***
     * 상품 등록 기능
     * @param dto 상품 등록 요청 ProductRequest
     * @param userId
     * @return 등록된 상품의 ID
     */
    @Transactional
    public Long createProduct(ProductRequest dto, String userId) {
        ProductCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid category ID"));

        Product product = Product.fromRequest(dto, category, Long.valueOf(userId));
        Product saved = productRepository.save(product);

        // 상품 이미지 id 연결
        if (dto.getImageFileIds() != null && !dto.getImageFileIds()
                .isEmpty()) {
            int sortOrder = 1;
            for (Long imageFileId : dto.getImageFileIds()) {
                ProductImageId id = new ProductImageId(saved.getId(), imageFileId);

                ProductImage image = ProductImage.builder()
                        .id(id)
                        .sortOrder(sortOrder++)
                        .build();

                productImageRepository.save(image);
            }
        }

        // Elasticsearch 색인
        indexProduct(saved);

        return saved.getId();
    }

    /***
     * Elasticsearch에 상품 데이터를 색인
     * @param product 색인할 상품 엔티티
     */
    public void indexProduct(Product product) {
        ProductDocument doc = ProductDocument.fromEntity(product);
        searchRepository.save(doc);
    }
}
