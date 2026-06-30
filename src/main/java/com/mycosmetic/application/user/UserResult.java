package com.mycosmetic.application.user;

import com.mycosmetic.domain.user.SkinType;
import com.mycosmetic.domain.user.User;

public record UserResult(
        String email,
        String nickname,
        SkinType skinType,
        String skinConcerns,
        String allergyIngredients
) {
    public static UserResult from(User user) {
        return new UserResult(
                user.getEmail(),
                user.getNickname(),
                user.getSkinType(),
                user.getSkinConcerns(),
                user.getAllergyIngredients()
        );
    }
}
