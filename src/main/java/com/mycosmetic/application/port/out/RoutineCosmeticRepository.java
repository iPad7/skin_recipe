package com.mycosmetic.application.port.out;

/**
 * RoutineCosmetic 영속성 outbound 포트. (RoutineCosmetic 자체는 Routine 저장 시 cascade 처리)
 * 구현체: {@code adapter.out.persistence.RoutineCosmeticPersistenceAdapter}
 */
public interface RoutineCosmeticRepository {

    boolean existsByCosmeticId(Long cosmeticId);
}
