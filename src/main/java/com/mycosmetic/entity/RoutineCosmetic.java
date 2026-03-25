package com.mycosmetic.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "routine_cosmetics")
@Getter
@NoArgsConstructor
public class RoutineCosmetic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id", nullable = false)
    private Routine routine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cosmetic_id", nullable = false)
    private Cosmetic cosmetic;

    @Column(name = "`order`", nullable = false)
    private int order;

    @Builder
    public RoutineCosmetic(Routine routine, Cosmetic cosmetic, int order) {
        this.routine = routine;
        this.cosmetic = cosmetic;
        this.order = order;
    }
}
