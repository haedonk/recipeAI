package com.haekitchenapp.recipeapp.repository;

import com.haekitchenapp.recipeapp.entity.MealType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MealTypeRepository extends JpaRepository<MealType, Short> {

    @Query("SELECT mt.name FROM MealType mt WHERE mt.id = :id")
    Optional<String> findNameById(@Param("id") Short id);
}
