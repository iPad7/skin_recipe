package com.mycosmetic.repository;

import com.mycosmetic.entity.RoutineCosmetic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineCosmeticRepository extends JpaRepository<RoutineCosmetic, Long> {
    boolean existsByCosmeticId(Long cosmeticId);
}
