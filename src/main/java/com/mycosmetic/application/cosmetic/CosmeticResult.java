package com.mycosmetic.application.cosmetic;

import com.mycosmetic.domain.cosmetic.Cosmetic;
import com.mycosmetic.domain.cosmetic.CosmeticCategory;

import java.time.LocalDateTime;

public record CosmeticResult(
        Long id,
        String name,
        String brand,
        CosmeticCategory category,
        String ingredients,
        String imageUrl,
        LocalDateTime createdAt
) {
    public static CosmeticResult from(Cosmetic cosmetic) {
        return new CosmeticResult(
                cosmetic.getId(),
                cosmetic.getName(),
                cosmetic.getBrand(),
                cosmetic.getCategory(),
                cosmetic.getIngredients(),
                cosmetic.getImageUrl(),
                cosmetic.getCreatedAt()
        );
    }
}
