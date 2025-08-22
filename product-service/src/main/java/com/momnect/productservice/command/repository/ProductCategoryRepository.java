package com.momnect.productservice.command.repository;

import com.momnect.productservice.command.entity.product.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    List<ProductCategory> findByParentIsNull();
}
