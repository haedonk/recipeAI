package com.haekitchenapp.recipeapp.service;

import com.haekitchenapp.recipeapp.entity.Recipe;
import com.haekitchenapp.recipeapp.entity.Unit;
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeIngredientRequest;
import com.haekitchenapp.recipeapp.model.response.ApiResponse;
import com.haekitchenapp.recipeapp.repository.UnitRepository;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UnitService {

    @Autowired
    private UnitRepository unitRepository;

    private Map<Long, String> unitCache;

    public ResponseEntity<ApiResponse<List<Unit>>> getAllUnits() {
        List<Unit> units = unitRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success("Units retrieved successfully", units));
    }

    public Map<Long, String> getAllUnitsMap() {
        if (unitCache == null) {
            unitCache = unitRepository.findAll()
                    .stream()
                    .collect(Collectors.toMap(Unit::getId, Unit::getName));
        }
        return unitCache;
    }

    private void refreshUnitCache() {
        unitCache = unitRepository.findAll()
                .stream()
                .collect(Collectors.toMap(Unit::getId, Unit::getName));
    }

    public String getUnitNameById(Long unitId) {
        if (unitCache == null || !unitCache.containsKey(unitId)) {
            unitCache = unitRepository.findAll()
                    .stream()
                    .collect(Collectors.toMap(Unit::getId, Unit::getName));
        }
        return unitCache.getOrDefault(unitId, "Unknown Unit");
    }

    public Unit getUnitByName(String unitName) {
        return unitRepository.findByName(unitName.toLowerCase()).orElse(null);
    }

    public boolean existsById(Long unitId) {
        if (unitId == null) {
            return false;
        }
        // Use the cache instead of making a database call
        if (unitCache == null) {
            unitCache = unitRepository.findAll()
                    .stream()
                    .collect(Collectors.toMap(Unit::getId, Unit::getName));
        }
        return unitCache.containsKey(unitId);
    }

    public void persistAiGeneratedUnits(Set<RecipeIngredientRequest> ingredients) {
        Set<String> uniqueUnitNames = ingredients.stream()
                .map(RecipeIngredientRequest::getUnitName)
                .filter(unitName -> unitName != null && !unitName.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<Unit> existingUnits = uniqueUnitNames.stream().map(this::getUnitByName)
                .filter(Objects::nonNull)
                .toList();

        List<Unit> newUnits = uniqueUnitNames.stream()
                .filter(unitName -> existingUnits.stream().noneMatch(u -> u.getName().equalsIgnoreCase(unitName)))
                .map(unitName -> {
                    Unit unit = new Unit();
                    unit.setName(unitName);
                    unit.setAiGenerated(true);
                    return unit;
                })
                .collect(Collectors.toList());

        if (!newUnits.isEmpty()) {
            unitRepository.saveAll(newUnits);
            log.info("Added {} new units: {}", newUnits.size(),
                    newUnits.stream().map(Unit::getName).collect(Collectors.joining(", ")));
            // Invalidate the cache
            refreshUnitCache();
        }
    }
}
