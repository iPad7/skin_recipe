package com.mycosmetic.adapter.out.persistence;

import com.mycosmetic.domain.cosmetic.Cosmetic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CosmeticRepository extends JpaRepository<Cosmetic, Long> {

    List<Cosmetic> findAllByUserId(Long userId);
}
