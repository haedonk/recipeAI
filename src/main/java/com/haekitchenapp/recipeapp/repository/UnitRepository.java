package com.haekitchenapp.recipeapp.repository;

import com.haekitchenapp.recipeapp.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UnitRepository extends JpaRepository<Unit, Long> {

    Optional<Unit> findByName(String unitName);
}
