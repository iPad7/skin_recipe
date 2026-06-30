package com.mycosmetic.adapter.in.web.dto.response;

import com.mycosmetic.application.user.UserResult;
import com.mycosmetic.domain.user.SkinType;
import lombok.Getter;

@Getter
public class UserResponse {

    private final String email;
    private final String nickname;
    private final SkinType skinType;
    private final String skinConcerns;
    private final String allergyIngredients;

    private UserResponse(String email, String nickname, SkinType skinType,
                         String skinConcerns, String allergyIngredients) {
        this.email = email;
        this.nickname = nickname;
        this.skinType = skinType;
        this.skinConcerns = skinConcerns;
        this.allergyIngredients = allergyIngredients;
    }

    public static UserResponse from(UserResult result) {
        return new UserResponse(result.email(), result.nickname(), result.skinType(),
                result.skinConcerns(), result.allergyIngredients());
    }
}
