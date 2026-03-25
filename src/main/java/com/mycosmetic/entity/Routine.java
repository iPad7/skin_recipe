package com.mycosmetic.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "routines")
@Getter
@NoArgsConstructor
public class Routine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeOfDay timeOfDay;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "routine", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("order ASC")
    private List<RoutineCosmetic> routineCosmetics = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Routine(User user, String name, TimeOfDay timeOfDay, String description) {
        this.user = user;
        this.name = name;
        this.timeOfDay = timeOfDay;
        this.description = description;
    }
}
