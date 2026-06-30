package com.mycosmetic.application.user;

import com.mycosmetic.domain.user.SkinType;

public record SignupCommand(
        String email,
        String password,
        String nickname,
        SkinType skinType,
        String skinConcerns,
        String allergyIngredients
) {}
