package com.mycosmetic.application.user;

public record LoginCommand(
        String email,
        String password
) {}
