package com.momnect.productservice.command.service;

import com.momnect.productservice.command.client.FileClient;
import com.momnect.productservice.command.dto.*;
import com.momnect.productservice.command.entity.*;
import com.momnect.productservice.command.repository.ProductCategoryRepository;
import com.momnect.productservice.command.repository.ProductImageRepository;
import com.momnect.productservice.command.repository.ProductRepository;
import com.momnect.productservice.command.repository.ProductSearchRepository;
import com.momnect.productservice.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final FileClient fileClient;

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductSearchRepository searchRepository;

    /***
     * 상품 상세 조회
     * @param productId 조회할 상품 ID
     * @return 상품 엔티티
     */
    @Transactional(readOnly = true)
    public ProductDTO getProduct(Long productId) {
        // 엔티티 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + productId));

        // 이미지 파일 ID 리스트 추출
        List<Long> imageIds = product.getProductImages()
                .stream()
                .map(img -> img.getId().getImageFileId())
                .toList();

        String idsParam = imageIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        ApiResponse<List<ImageFileDTO>> response = fileClient.getImageFilesByIds(idsParam);

        Map<Long, String> paths = response.getData().stream()
                .collect(Collectors.toMap(ImageFileDTO::getId, ImageFileDTO::getPath));

        List<ProductImageDTO> images = product.getProductImages().stream()
                .map(img -> ProductImageDTO.builder()
                        .imageFileId(img.getId().getImageFileId())
                        .sortOrder(img.getSortOrder())
                        .url(paths.get(img.getId().getImageFileId()))
                        .build())
                .collect(Collectors.toList());


        return ProductDTO.fromEntity(product, images);
    }

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
