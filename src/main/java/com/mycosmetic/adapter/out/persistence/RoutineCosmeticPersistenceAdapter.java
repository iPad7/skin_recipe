package com.mycosmetic.adapter.out.persistence;

import com.mycosmetic.application.port.out.RoutineCosmeticRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RoutineCosmeticPersistenceAdapter implements RoutineCosmeticRepository {

    private final RoutineCosmeticJpaRepository jpa;

    @Override
    public boolean existsByCosmeticId(Long cosmeticId) {
        return jpa.existsByCosmeticId(cosmeticId);
    }
}
