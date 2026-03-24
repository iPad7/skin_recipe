package com.mycosmetic.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cosmetics")
@Getter
@NoArgsConstructor
public class Cosmetic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CosmeticCategory category;

    @Column(columnDefinition = "TEXT")
    private String ingredients;

    private String imageUrl;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Cosmetic(User user, String name, String brand,
                    CosmeticCategory category, String ingredients, String imageUrl) {
        this.user = user;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.ingredients = ingredients;
        this.imageUrl = imageUrl;
    }

    public void update(String name, String brand, CosmeticCategory category, String ingredients) {
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.ingredients = ingredients;
    }
}
