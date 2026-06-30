package com.mycosmetic.application.port.out;

import com.mycosmetic.domain.cosmetic.Cosmetic;

import java.util.List;
import java.util.Optional;

/**
 * Cosmetic 영속성 outbound 포트.
 * 구현체: {@code adapter.out.persistence.CosmeticPersistenceAdapter}
 */
public interface CosmeticRepository {

    Cosmetic save(Cosmetic cosmetic);

    Optional<Cosmetic> findById(Long id);

    List<Cosmetic> findAll();

    List<Cosmetic> findAllById(Iterable<Long> ids);

    List<Cosmetic> findAllByUserId(Long userId);

    void delete(Cosmetic cosmetic);

    void deleteAll(Iterable<? extends Cosmetic> entities);
}
