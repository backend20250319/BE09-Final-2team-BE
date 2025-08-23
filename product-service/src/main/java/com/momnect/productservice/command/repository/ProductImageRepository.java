package com.momnect.productservice.command.repository;

import com.momnect.productservice.command.entity.ProductImage;
import com.momnect.productservice.command.entity.ProductImageId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, ProductImageId> {
}

