package com.haekitchenapp.recipeapp.service;

import com.haekitchenapp.recipeapp.entity.Recipe;
import com.haekitchenapp.recipeapp.entity.Unit;
import com.haekitchenapp.recipeapp.model.response.ApiResponse;
import com.haekitchenapp.recipeapp.repository.UnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UnitService {

    @Autowired
    private UnitRepository unitRepository;

    private Map<Long, String> unitCache;

    public ResponseEntity<ApiResponse<List<Unit>>> getAllUnits() {
        List<Unit> units = unitRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success("Units retrieved successfully", units));
    }

    public String getUnitNameById(Long unitId) {
        if (unitCache == null || !unitCache.containsKey(unitId)) {
            unitCache = unitRepository.findAll()
                    .stream()
                    .collect(Collectors.toMap(Unit::getId, Unit::getName));
        }
        return unitCache.getOrDefault(unitId, "Unknown Unit");
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
}
