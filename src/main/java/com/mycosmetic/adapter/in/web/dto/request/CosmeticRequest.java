package com.mycosmetic.adapter.in.web.dto.request;

import com.mycosmetic.domain.cosmetic.CosmeticCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CosmeticRequest {

    @NotBlank
    private String name;

    private String brand;

    @NotNull
    private CosmeticCategory category;

    private String ingredients;
}
