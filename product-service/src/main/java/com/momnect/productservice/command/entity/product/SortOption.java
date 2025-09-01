package com.momnect.productservice.command.entity.product;

import lombok.Getter;

@Getter
public enum SortOption {
    RECOMMENDED("추천순"),
    LATEST("최신순"),
    PRICE_ASC("낮은가격순"),
    PRICE_DESC("높은가격순");

    private final String label;

    SortOption(String label) {
        this.label = label;
    }

    public static String getLabel(SortOption option) {
        return option.getLabel();
    }
}
