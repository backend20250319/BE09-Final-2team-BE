package com.momnect.productservice.command.repository;

import com.momnect.productservice.command.entity.area.ProductTradeArea;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductTradeAreaRepository extends JpaRepository<ProductTradeArea, Long> {
}
