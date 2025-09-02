package com.momnect.reviewservice.command.client;

import com.momnect.reviewservice.command.dto.ReviewDTO;
import com.momnect.reviewservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "http://localhost:8761", configuration = FeignClientConfig.class)
public interface ProductClient {

    @GetMapping("/api/v1/product-service/products/{productId}")
    ReviewDTO getProductInfo(@PathVariable("productId") Long productId);
}
