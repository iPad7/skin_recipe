package com.mycosmetic.application.cosmetic;

import com.mycosmetic.domain.cosmetic.CosmeticCategory;

public record UpdateCosmeticCommand(
        String name,
        String brand,
        CosmeticCategory category,
        String ingredients
) {}
