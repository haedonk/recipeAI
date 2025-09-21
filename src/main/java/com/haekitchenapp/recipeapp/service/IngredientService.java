package com.haekitchenapp.recipeapp.service;

import com.haekitchenapp.recipeapp.entity.Ingredient;
import com.haekitchenapp.recipeapp.exception.IngredientException;
import com.haekitchenapp.recipeapp.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientRepository ingredientRepository;

    private Map<Long, Ingredient> ingredientCache = new ConcurrentHashMap<>();
    private Map<String, Ingredient> ingredientNameCache = new ConcurrentHashMap<>();

    private long countOnLoad = 0;
    private long lastIdOnLoad = 0;

    @EventListener(ApplicationReadyEvent.class)
    private void initializeCache() {
        loadCache(0);
        log.info("Ingredient cache initialized with {} ingredients.", countOnLoad);
    }

    private void loadCache(long startingId) {
        if (ingredientCache.isEmpty()) {
            List<Ingredient> ingredients = fetchNewIngredients(startingId);
            countOnLoad = ingredients.size();
            if (!ingredients.isEmpty()) {
                lastIdOnLoad = ingredients.stream()
                        .mapToLong(ingredient -> ingredient.getId().intValue())
                        .max()
                        .orElse(0);
            }
            ingredientCache = ingredients.stream()
                    .collect(Collectors.toMap(Ingredient::getId, ingredient -> ingredient));
            ingredientNameCache = ingredients.stream()
                    .collect(Collectors.toMap(
                            ingredient -> ingredient.getName().toLowerCase(),
                            ingredient -> ingredient
                    ));
        }
    }

    private List<Ingredient> fetchNewIngredients(long lastId) {
        return ingredientRepository.findAllGreaterThanId(lastId);
    }

    private boolean refreshCacheIfNeeded() {
        if (sizeChanged()) {
            loadCache(lastIdOnLoad);
            return true;
        }
        return false;
    }

    private boolean sizeChanged() {
        long currentCount = ingredientRepository.count();
        if (currentCount != countOnLoad) {
            countOnLoad = currentCount;
            return true;
        }
        return false;
    }

    public Ingredient getIngredientElseInsert(String ingredientName) {
        if (ingredientName == null || ingredientName.trim().isEmpty()) {
            throw new IngredientException("Ingredient name cannot be null or empty");
        }
        String normalizedName = ingredientName.trim().toLowerCase();
        Ingredient ingredient = getIngredientRefreshIfNeeded(normalizedName);
        if (ingredient != null) {
            return ingredient;
        } else {
            Ingredient newIngredient = new Ingredient();
            newIngredient.setName(normalizedName);
            Ingredient savedIngredient = ingredientRepository.save(newIngredient);
            // Update caches
            ingredientCache.put(savedIngredient.getId(), savedIngredient);
            ingredientNameCache.put(normalizedName, savedIngredient);
            lastIdOnLoad = Math.max(lastIdOnLoad, savedIngredient.getId());
            countOnLoad++;
            return savedIngredient;
        }
    }

    private Ingredient getIngredientRefreshIfNeeded(String name) {
        if (name == null) {
            throw new IngredientException("Ingredient name cannot be null");
        }
        boolean refreshed = false;
        Ingredient ingredient = ingredientNameCache.get(name);
        if (ingredient == null) {
            refreshed = refreshCacheIfNeeded();
        }
        if (refreshed) {
            ingredient = ingredientNameCache.get(name);
        }
        return ingredient;
    }

    public String getIngredientNameById(Long ingredientId) {
        if (ingredientId == null) {
            throw new IngredientException("Ingredient ID cannot be null");
        }
        boolean refreshed = false;
        Ingredient ingredient = ingredientCache.get(ingredientId);
        if (ingredient == null) {
            refreshed = refreshCacheIfNeeded();
        }
        if (refreshed) {
            ingredient = ingredientCache.get(ingredientId);
        }
        return ingredient != null ? ingredient.getName() : null;
    }

    public String getIngredientNameByName(String ingredientName) {
        if (ingredientName == null) {
            throw new IngredientException("Ingredient name cannot be null");
        }
        Ingredient ingredient = ingredientNameCache.get(ingredientName.toLowerCase());
        if (ingredient != null) {
            return ingredient.getName();
        } else {
            return null;
        }
    }
}
