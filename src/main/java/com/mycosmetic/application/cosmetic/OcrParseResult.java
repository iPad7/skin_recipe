package com.mycosmetic.application.cosmetic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mycosmetic.domain.cosmetic.CosmeticCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OcrParseResult {

    private String name;
    private String brand;
    private String category;    // LLM이 반환하는 원본 문자열 그대로 받음
    private String ingredients;
    private String confidence;  // "high" | "low"
    @lombok.Setter
    private String imageUrl;

    @JsonIgnore
    public boolean isHighConfidence() {
        return "high".equalsIgnoreCase(confidence);
    }

    /** LLM 응답이 유효하지 않은 값일 경우 ETC로 fallback */
    public CosmeticCategory toCosmeticCategory() {
        if (category == null) return CosmeticCategory.ETC;
        try {
            return CosmeticCategory.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CosmeticCategory.ETC;
        }
    }
}
