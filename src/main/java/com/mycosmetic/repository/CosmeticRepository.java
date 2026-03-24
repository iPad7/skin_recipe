package com.mycosmetic.repository;

import com.mycosmetic.entity.Cosmetic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CosmeticRepository extends JpaRepository<Cosmetic, Long> {

    List<Cosmetic> findAllByUserId(Long userId);
}
