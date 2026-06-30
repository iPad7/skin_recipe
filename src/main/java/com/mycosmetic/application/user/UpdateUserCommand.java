package com.mycosmetic.application.user;

import com.mycosmetic.domain.user.SkinType;

public record UpdateUserCommand(
        String nickname,
        SkinType skinType,
        String skinConcerns,
        String allergyIngredients
) {}
