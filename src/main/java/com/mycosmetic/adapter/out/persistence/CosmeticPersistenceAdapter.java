package com.mycosmetic.adapter.out.persistence;

import com.mycosmetic.application.port.out.CosmeticRepository;
import com.mycosmetic.domain.cosmetic.Cosmetic;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CosmeticPersistenceAdapter implements CosmeticRepository {

    private final CosmeticJpaRepository jpa;

    @Override
    public Cosmetic save(Cosmetic cosmetic) {
        return jpa.save(cosmetic);
    }

    @Override
    public Optional<Cosmetic> findById(Long id) {
        return jpa.findById(id);
    }

    @Override
    public List<Cosmetic> findAll() {
        return jpa.findAll();
    }

    @Override
    public List<Cosmetic> findAllById(Iterable<Long> ids) {
        return jpa.findAllById(ids);
    }

    @Override
    public List<Cosmetic> findAllByUserId(Long userId) {
        return jpa.findAllByUserId(userId);
    }

    @Override
    public void delete(Cosmetic cosmetic) {
        jpa.delete(cosmetic);
    }

    @Override
    public void deleteAll(Iterable<? extends Cosmetic> entities) {
        jpa.deleteAll(entities);
    }
}
