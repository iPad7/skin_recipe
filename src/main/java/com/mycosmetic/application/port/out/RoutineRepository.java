package com.mycosmetic.application.port.out;

import com.mycosmetic.domain.routine.Routine;

import java.util.List;
import java.util.Optional;

/**
 * Routine 영속성 outbound 포트.
 * 구현체: {@code adapter.out.persistence.RoutinePersistenceAdapter}
 */
public interface RoutineRepository {

    Routine save(Routine routine);

    Optional<Routine> findById(Long id);

    List<Routine> findAllByUserId(Long userId);

    List<Routine> findAllByUserIdWithCosmetics(Long userId);

    void delete(Routine routine);

    void deleteAll(Iterable<? extends Routine> entities);
}
