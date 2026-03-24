package com.mycosmetic.dto.request;

import com.mycosmetic.entity.CosmeticCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OcrConfirmRequest {

    @NotBlank
    private String name;

    private String brand;

    @NotNull
    private CosmeticCategory category;

    private String ingredients;

    private String imageUrl;
}
