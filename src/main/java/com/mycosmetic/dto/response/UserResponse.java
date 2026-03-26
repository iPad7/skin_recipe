package com.mycosmetic.dto.response;

import com.mycosmetic.entity.SkinType;
import com.mycosmetic.entity.User;
import lombok.Getter;

@Getter
public class UserResponse {

    private final String email;
    private final String nickname;
    private final SkinType skinType;
    private final String skinConcerns;
    private final String allergyIngredients;

    public UserResponse(User user) {
        this.email = user.getEmail();
        this.nickname = user.getNickname();
        this.skinType = user.getSkinType();
        this.skinConcerns = user.getSkinConcerns();
        this.allergyIngredients = user.getAllergyIngredients();
    }
}
