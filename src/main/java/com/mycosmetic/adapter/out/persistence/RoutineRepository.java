package com.mycosmetic.adapter.out.persistence;

import com.mycosmetic.domain.routine.Routine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoutineRepository extends JpaRepository<Routine, Long> {
    List<Routine> findAllByUserId(Long userId);

    @Query("SELECT DISTINCT r FROM Routine r LEFT JOIN FETCH r.routineCosmetics rc LEFT JOIN FETCH rc.cosmetic WHERE r.user.id = :userId")
    List<Routine> findAllByUserIdWithCosmetics(@Param("userId") Long userId);
}
