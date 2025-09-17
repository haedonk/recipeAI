package com.haekitchenapp.recipeapp.repository;

import com.haekitchenapp.recipeapp.entity.LlmQueryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LlmQueryLogRepository extends JpaRepository<LlmQueryLog, Long> {
    // You can add custom query methods here if needed, for example:
    // List<LlmQueryLog> findByModel(String model);
    Optional<LlmQueryLog> findById(String id);
}
