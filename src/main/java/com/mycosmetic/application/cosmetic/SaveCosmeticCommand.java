package com.mycosmetic.application.cosmetic;

import com.mycosmetic.domain.cosmetic.CosmeticCategory;

public record SaveCosmeticCommand(
        String name,
        String brand,
        CosmeticCategory category,
        String ingredients,
        String imageUrl
) {}
