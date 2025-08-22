package com.momnect.productservice.command.repository;

import com.momnect.productservice.command.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

}

