package com.mycosmetic.adapter.out.persistence;

import com.mycosmetic.domain.routine.RoutineCosmetic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineCosmeticJpaRepository extends JpaRepository<RoutineCosmetic, Long> {
    boolean existsByCosmeticId(Long cosmeticId);
}
