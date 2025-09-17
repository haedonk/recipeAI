package com.haekitchenapp.recipeapp.repository;

import com.haekitchenapp.recipeapp.entity.LlmModelPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface LlmModelPriceRepository extends JpaRepository<LlmModelPrice, String> {

    /**
     * Find all model prices that became effective after a given date
     *
     * @param date The date to check against
     * @return List of LlmModelPrice entities
     */
    List<LlmModelPrice> findByEffectiveFromAfter(ZonedDateTime date);

    /**
     * Find all model prices ordered by effective date (newest first)
     *
     * @return List of LlmModelPrice entities
     */
    List<LlmModelPrice> findAllByOrderByEffectiveFromDesc();
}
