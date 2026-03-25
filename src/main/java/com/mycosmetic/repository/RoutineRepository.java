package com.mycosmetic.repository;

import com.mycosmetic.entity.Routine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutineRepository extends JpaRepository<Routine, Long> {
    List<Routine> findAllByUserId(Long userId);
}
