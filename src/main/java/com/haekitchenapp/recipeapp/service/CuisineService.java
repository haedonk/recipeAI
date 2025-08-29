package com.haekitchenapp.recipeapp.service;
import com.haekitchenapp.recipeapp.entity.Cuisine;

import java.util.List;
import java.util.Optional;

public interface CuisineService {

    /**
     * Find a cuisine by its ID
     * @param id the cuisine ID
     * @return the cuisine if found
     */
    Optional<Cuisine> findById(Integer id);

    /**
     * Find a cuisine by its name
     * @param name the cuisine name
     * @return the cuisine if found
     */
    Optional<Cuisine> findByName(String name);

    /**
     * Get all cuisines
     * @return list of all cuisines
     */
    List<Cuisine> findAll();

    /**
     * Find cuisines with names containing the given text (case insensitive)
     * @param name partial name to search for
     * @return list of matching cuisines
     */
    List<Cuisine> findByNameContaining(String name);

    /**
     * Find cuisines associated with a specific recipe
     * @param recipeId the recipe ID
     * @return list of cuisines associated with the recipe
     */
    List<Cuisine> findByRecipeId(Long recipeId);

    /**
     * Create a new cuisine
     * @param cuisine the cuisine to create
     * @return the created cuisine
     */
    Cuisine createCuisine(Cuisine cuisine);

    /**
     * Update an existing cuisine
     * @param id the cuisine ID
     * @param cuisine the cuisine data to update
     * @return the updated cuisine
     */
    Cuisine updateCuisine(Integer id, Cuisine cuisine);

    /**
     * Delete a cuisine
     * @param id the cuisine ID to delete
     */
    void deleteCuisine(Integer id);

    /**
     * Check if a cuisine with the given name exists
     * @param name the cuisine name
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);
}

