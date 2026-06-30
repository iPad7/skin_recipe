package com.mycosmetic.adapter.out.persistence;

import com.mycosmetic.application.port.out.RoutineRepository;
import com.mycosmetic.domain.routine.Routine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoutinePersistenceAdapter implements RoutineRepository {

    private final RoutineJpaRepository jpa;

    @Override
    public Routine save(Routine routine) {
        return jpa.save(routine);
    }

    @Override
    public Optional<Routine> findById(Long id) {
        return jpa.findById(id);
    }

    @Override
    public List<Routine> findAllByUserId(Long userId) {
        return jpa.findAllByUserId(userId);
    }

    @Override
    public List<Routine> findAllByUserIdWithCosmetics(Long userId) {
        return jpa.findAllByUserIdWithCosmetics(userId);
    }

    @Override
    public void delete(Routine routine) {
        jpa.delete(routine);
    }

    @Override
    public void deleteAll(Iterable<? extends Routine> entities) {
        jpa.deleteAll(entities);
    }
}
