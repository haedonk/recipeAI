package com.haekitchenapp.recipeapp.controller;

import com.haekitchenapp.recipeapp.entity.Cuisine;
import com.haekitchenapp.recipeapp.exception.CuisineNotFoundException;
import com.haekitchenapp.recipeapp.service.CuisineService;
import com.haekitchenapp.recipeapp.service.RecipeCuisineService;
import com.haekitchenapp.recipeapp.service.RecipeMealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * Controller for managing cuisines and their associations with recipes
 */
@RestController
@Validated
@RequestMapping("/api/cuisines")
@Slf4j
@RequiredArgsConstructor
public class CuisineController {

    private final CuisineService cuisineService;
    private final RecipeCuisineService recipeCuisineService;
    private final RecipeMealService recipeMealService;

    /**
     * Get all cuisines
     * @return list of all cuisines
     */
    @GetMapping
    public ResponseEntity<List<Cuisine>> getAllCuisines() {
        log.info("Getting all cuisines");
        return ResponseEntity.ok(cuisineService.findAll());
    }

    /**
     * Get a specific cuisine by ID
     * @param id cuisine ID
     * @return the cuisine
     */
    @GetMapping("/{id}")
    public ResponseEntity<Cuisine> getCuisineById(@PathVariable Integer id) {
        log.info("Getting cuisine with ID: {}", id);
        return cuisineService.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new CuisineNotFoundException("Cuisine not found with id: " + id));
    }

    /**
     * Create a new cuisine
     * @param cuisine the cuisine to create
     * @return the created cuisine
     */
    @PostMapping
    public ResponseEntity<Cuisine> createCuisine(@Valid @RequestBody Cuisine cuisine) {
        log.info("Creating new cuisine: {}", cuisine.getName());
        return new ResponseEntity<>(cuisineService.createCuisine(cuisine), HttpStatus.CREATED);
    }

    /**
     * Update an existing cuisine
     * @param id cuisine ID
     * @param cuisine the updated cuisine data
     * @return the updated cuisine
     */
    @PutMapping("/{id}")
    public ResponseEntity<Cuisine> updateCuisine(@PathVariable Integer id, @Valid @RequestBody Cuisine cuisine) {
        log.info("Updating cuisine with ID: {}", id);
        return ResponseEntity.ok(cuisineService.updateCuisine(id, cuisine));
    }

    /**
     * Delete a cuisine
     * @param id cuisine ID
     * @return no content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCuisine(@PathVariable Integer id) {
        log.info("Deleting cuisine with ID: {}", id);
        cuisineService.deleteCuisine(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Search cuisines by name
     * @param name name to search for
     * @return list of matching cuisines
     */
    @GetMapping("/search")
    public ResponseEntity<List<Cuisine>> searchCuisinesByName(@RequestParam String name) {
        log.info("Searching cuisines with name containing: {}", name);
        return ResponseEntity.ok(cuisineService.findByNameContaining(name));
    }

    /**
     * Get all cuisines associated with a recipe
     * @param recipeId recipe ID
     * @return list of cuisines
     */
    @GetMapping("/recipe/{recipeId}")
    public ResponseEntity<List<String>> getCuisinesForRecipe(@PathVariable Long recipeId) {
        log.info("Getting cuisines for recipe ID: {}", recipeId);
        return ResponseEntity.ok(recipeCuisineService.getRecipeCuisineList(recipeId));
    }

    /**
     * Associate a recipe with a cuisine
     * @param recipeId recipe ID
     * @param cuisineId cuisine ID
     * @return success message
     */
    @PostMapping("/recipe/{recipeId}/cuisine/{cuisineId}")
    public ResponseEntity<String> associateRecipeWithCuisine(
            @PathVariable Long recipeId,
            @PathVariable Integer cuisineId) {
        log.info("Associating recipe ID: {} with cuisine ID: {}", recipeId, cuisineId);
        recipeCuisineService.associateRecipeWithCuisine(recipeId, cuisineId);
        return ResponseEntity.ok("Recipe successfully associated with cuisine");
    }

    /**
     * Associate a recipe with multiple cuisines
     * @param recipeId recipe ID
     * @param cuisineIds list of cuisine IDs
     * @return success message
     */
    @PostMapping("/recipe/{recipeId}/cuisines")
    public ResponseEntity<String> associateRecipeWithCuisines(
            @PathVariable Long recipeId,
            @Valid @RequestBody Set<Integer> cuisineIds) {
        log.info("Associating recipe ID: {} with multiple cuisines", recipeId);
        recipeCuisineService.associateRecipeWithCuisines(recipeId, cuisineIds);
        return ResponseEntity.ok("Recipe successfully associated with cuisines");
    }

    /**
     * Update all cuisine associations for a recipe
     * @param recipeId recipe ID
     * @param cuisineIds list of cuisine IDs
     * @return success message
     */
    @PutMapping("/recipe/{recipeId}/cuisines")
    public ResponseEntity<String> updateRecipeCuisines(
            @PathVariable Long recipeId,
            @Valid @RequestBody Set<Integer> cuisineIds) {
        log.info("Updating cuisines for recipe ID: {}", recipeId);
        recipeCuisineService.updateRecipeCuisines(recipeId, cuisineIds);
        return ResponseEntity.ok("Recipe cuisines updated successfully");
    }

    /**
     * Remove an association between a recipe and a cuisine
     * @param recipeId recipe ID
     * @param cuisineId cuisine ID
     * @return success message
     */
    @DeleteMapping("/recipe/{recipeId}/cuisine/{cuisineId}")
    public ResponseEntity<String> removeRecipeCuisine(
            @PathVariable Long recipeId,
            @PathVariable Integer cuisineId) {
        log.info("Removing association for recipe ID: {} and cuisine ID: {}", recipeId, cuisineId);
        recipeCuisineService.removeRecipeCuisine(recipeId, cuisineId);
        return ResponseEntity.ok("Association removed successfully");
    }

    /**
     * Remove all cuisine associations for a recipe
     * @param recipeId recipe ID
     * @return success message
     */
    @DeleteMapping("/recipe/{recipeId}/cuisines")
    public ResponseEntity<String> removeAllRecipeCuisines(@PathVariable Long recipeId) {
        log.info("Removing all cuisine associations for recipe ID: {}", recipeId);
        recipeCuisineService.removeAllRecipeCuisines(recipeId);
        return ResponseEntity.ok("All cuisine associations removed successfully");
    }

    /**
     * Check if a recipe is associated with a cuisine
     * @param recipeId recipe ID
     * @param cuisineId cuisine ID
     * @return true if associated, false otherwise
     */
    @GetMapping("/recipe/{recipeId}/cuisine/{cuisineId}")
    public ResponseEntity<Boolean> isRecipeAssociatedWithCuisine(
            @PathVariable Long recipeId,
            @PathVariable Integer cuisineId) {
        log.info("Checking if recipe ID: {} is associated with cuisine ID: {}", recipeId, cuisineId);
        return ResponseEntity.ok(recipeCuisineService.isRecipeAssociatedWithCuisine(recipeId, cuisineId));
    }

    @PostMapping("/recipe/{recipeId}/mealtypes")
    public ResponseEntity<String> associateRecipeWithMealTypes(
            @PathVariable Long recipeId,
            @Valid @RequestBody Set<Short> cuisineIds) {
        log.info("Associating recipe ID: {} with multiple cuisines", recipeId);
        recipeMealService.associateRecipeWithmealTypes(recipeId, cuisineIds);
        return ResponseEntity.ok("Recipe successfully associated with meal types");
    }
}
