package com.mycosmetic.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkinType skinType;

    // 예: "ACNE,PORE,MOISTURE"
    private String skinConcerns;

    // 예: "향료, 알코올, 살리실산"
    private String allergyIngredients;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public User(String email, String password, String nickname,
                SkinType skinType, String skinConcerns, String allergyIngredients) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.skinType = skinType;
        this.skinConcerns = skinConcerns;
        this.allergyIngredients = allergyIngredients;
    }

    public void update(String nickname, SkinType skinType, String skinConcerns, String allergyIngredients) {
        this.nickname = nickname;
        this.skinType = skinType;
        this.skinConcerns = skinConcerns;
        this.allergyIngredients = allergyIngredients;
    }
}
