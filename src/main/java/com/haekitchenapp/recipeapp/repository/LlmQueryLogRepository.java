package com.haekitchenapp.recipeapp.repository;

import com.haekitchenapp.recipeapp.entity.LlmQueryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LlmQueryLogRepository extends JpaRepository<LlmQueryLog, Long> {
    // You can add custom query methods here if needed, for example:
    // List<LlmQueryLog> findByModel(String model);
}
