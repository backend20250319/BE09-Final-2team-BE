package com.momnect.productservice.command.controller;

import com.momnect.productservice.command.dto.ProductRequest;
import com.momnect.productservice.command.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody ProductRequest dto) {
        Long productId = productService.createProduct(dto);
        return ResponseEntity.ok(productId);
    }
}
