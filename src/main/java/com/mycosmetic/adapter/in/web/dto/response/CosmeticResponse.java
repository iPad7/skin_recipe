package com.mycosmetic.adapter.in.web.dto.response;

import com.mycosmetic.application.cosmetic.CosmeticResult;
import com.mycosmetic.domain.cosmetic.CosmeticCategory;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CosmeticResponse {

    private final Long id;
    private final String name;
    private final String brand;
    private final CosmeticCategory category;
    private final String ingredients;
    private final String imageUrl;
    private final LocalDateTime createdAt;

    private CosmeticResponse(Long id, String name, String brand, CosmeticCategory category,
                             String ingredients, String imageUrl, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.ingredients = ingredients;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
    }

    public static CosmeticResponse from(CosmeticResult result) {
        return new CosmeticResponse(result.id(), result.name(), result.brand(), result.category(),
                result.ingredients(), result.imageUrl(), result.createdAt());
    }
}
