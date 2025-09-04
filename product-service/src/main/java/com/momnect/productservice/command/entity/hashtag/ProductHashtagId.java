package com.momnect.productservice.command.entity.hashtag;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductHashtagId implements Serializable {
    private Long productId;
    private Long hashtagId;
}
