package com.mycosmetic.adapter.in.web.dto.request;

import com.mycosmetic.domain.user.SkinType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateUserRequest {

    @NotBlank
    private String nickname;

    @NotNull
    private SkinType skinType;

    private String skinConcerns;

    private String allergyIngredients;
}
