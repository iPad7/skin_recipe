package com.mycosmetic.adapter.in.web.dto.response;

import com.mycosmetic.domain.cosmetic.Cosmetic;
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

    public CosmeticResponse(Cosmetic cosmetic) {
        this.id = cosmetic.getId();
        this.name = cosmetic.getName();
        this.brand = cosmetic.getBrand();
        this.category = cosmetic.getCategory();
        this.ingredients = cosmetic.getIngredients();
        this.imageUrl = cosmetic.getImageUrl();
        this.createdAt = cosmetic.getCreatedAt();
    }
}
