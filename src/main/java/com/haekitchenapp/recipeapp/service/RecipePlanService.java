package com.haekitchenapp.recipeapp.service;

import com.haekitchenapp.recipeapp.entity.MealType;
import com.haekitchenapp.recipeapp.entity.Recipe;
import com.haekitchenapp.recipeapp.entity.RecipePlan;
import com.haekitchenapp.recipeapp.entity.User;
import com.haekitchenapp.recipeapp.exception.RecipeNotFoundException;
import com.haekitchenapp.recipeapp.exception.UserNotFoundException;
import com.haekitchenapp.recipeapp.model.request.recipe.BulkRecipePlanRequest;
import com.haekitchenapp.recipeapp.model.response.ApiResponse;
import com.haekitchenapp.recipeapp.model.response.RecipePlanSimple;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipePlanResponse;
import com.haekitchenapp.recipeapp.repository.MealTypeRepository;
import com.haekitchenapp.recipeapp.repository.RecipePlanRepository;
import com.haekitchenapp.recipeapp.repository.RecipeRepository;
import com.haekitchenapp.recipeapp.repository.UserRepository;
import com.haekitchenapp.recipeapp.utility.RecipePlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipePlanService {

    private final RecipePlanRepository recipePlanRepository;
    private final UserService userService;
    private final RecipeRepository recipeRepository;
    private final MealTypeRepository mealTypeRepository;
    private final RecipePlanMapper recipePlanMapper;

    /**
     * Get a recipe plan by ID
     *
     * @param id The recipe plan ID
     * @return Optional containing the recipe plan if found
     */
    public Optional<RecipePlan> findById(Long id) {
        return recipePlanRepository.findById(id);
    }

    /**
     * Get all recipe plans for a user
     *
     * @param userId The user ID
     * @return List of recipe plans
     */
    public List<RecipePlan> findByUserId(Long userId) {
        return recipePlanRepository.findByUserId(userId);
    }

    /**
     * Get all recipe plans for a user on a specific date
     *
     * @param userId The user ID
     * @param date The plan date
     * @return List of recipe plans
     */
    public List<RecipePlan> findByUserIdAndDate(Long userId, LocalDate date) {
        return recipePlanRepository.findByUserIdAndPlanDate(userId, date);
    }

    /**
     * Get all recipe plans for a user within a date range
     *
     * @param userId The user ID
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of recipe plans
     */
    public List<RecipePlanSimple> findByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return recipePlanRepository.findByUserIdAndPlanDateBetween(userId, startDate, endDate);
    }

    /**
     * Create a new recipe plan
     *
     * @param recipePlan The recipe plan to create
     * @return The created recipe plan
     */
    @Transactional
    public RecipePlan createRecipePlan(RecipePlan recipePlan) {
        log.info("Creating recipe plan for user ID: {} on date: {}",
                recipePlan.getUser().getId(), recipePlan.getPlanDate());
        return recipePlanRepository.save(recipePlan);
    }

    /**
     * Create a recipe plan with a recipe
     *
     * @param userId The user ID
     * @param recipeId The recipe ID
     * @param mealTypeId The meal type ID
     * @param planDate The plan date
     * @param notes Optional notes
     * @return The created recipe plan
     */
    @Transactional
    public RecipePlan createRecipePlanWithRecipe(
            Long userId, Long recipeId, Short mealTypeId, LocalDate planDate, String notes) {

        User user = userService.getUserById(userId);

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found with id: " + recipeId));

        MealType mealType = mealTypeRepository.findById(mealTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Meal type not found with id: " + mealTypeId));

        RecipePlan recipePlan = new RecipePlan();
        recipePlan.setUser(user);
        recipePlan.setRecipe(recipe);
        recipePlan.setMealType(mealType);
        recipePlan.setPlanDate(planDate);
        recipePlan.setNotes(notes);

        log.info("Creating recipe plan with recipe for user ID: {} on date: {}", userId, planDate);
        return recipePlanRepository.save(recipePlan);
    }

    /**
     * Create a custom recipe plan (without a recipe)
     *
     * @param userId The user ID
     * @param customTitle Custom title for the plan
     * @param mealTypeId The meal type ID
     * @param planDate The plan date
     * @param notes Optional notes
     * @return The created recipe plan
     */
    @Transactional
    public RecipePlan createCustomRecipePlan(
            Long userId, String customTitle, Short mealTypeId, LocalDate planDate, String notes) {

        User user = userService.getUserById(userId);

        MealType mealType = mealTypeRepository.findById(mealTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Meal type not found with id: " + mealTypeId));

        RecipePlan recipePlan = new RecipePlan();
        recipePlan.setUser(user);
        recipePlan.setCustomTitle(customTitle);
        recipePlan.setMealType(mealType);
        recipePlan.setPlanDate(planDate);
        recipePlan.setNotes(notes);

        log.info("Creating custom recipe plan for user ID: {} on date: {}", userId, planDate);
        return recipePlanRepository.save(recipePlan);
    }

    /**
     * Update an existing recipe plan
     *
     * @param id The recipe plan ID
     * @param updatedPlan The updated recipe plan data
     * @return The updated recipe plan
     */
    @Transactional
    public RecipePlan updateRecipePlan(Long id, RecipePlan updatedPlan) {
        RecipePlan existingPlan = recipePlanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recipe plan not found with id: " + id));

        // Update fields that can be changed
        if (updatedPlan.getMealType() != null) {
            existingPlan.setMealType(updatedPlan.getMealType());
        }

        if (updatedPlan.getRecipe() != null) {
            existingPlan.setRecipe(updatedPlan.getRecipe());
        } else {
            existingPlan.setRecipe(null);
        }

        if (updatedPlan.getCustomTitle() != null) {
            existingPlan.setCustomTitle(updatedPlan.getCustomTitle());
        }

        if (updatedPlan.getNotes() != null) {
            existingPlan.setNotes(updatedPlan.getNotes());
        }

        if (updatedPlan.getPlanDate() != null) {
            existingPlan.setPlanDate(updatedPlan.getPlanDate());
        }

        log.info("Updating recipe plan ID: {}", id);
        return recipePlanRepository.save(existingPlan);
    }

    /**
     * Delete a recipe plan
     *
     * @param id The recipe plan ID
     */
    @Transactional
    public void deleteRecipePlan(Long id) {
        if (!recipePlanRepository.existsById(id)) {
            throw new IllegalArgumentException("Recipe plan not found with id: " + id);
        }

        log.info("Deleting recipe plan ID: {}", id);
        recipePlanRepository.deleteById(id);
    }

    /**
     * Delete all recipe plans for a user on a specific date
     *
     * @param userId The user ID
     * @param date The plan date
     */
    @Transactional
    public void deleteAllRecipePlansForUserOnDate(Long userId, LocalDate date) {
        log.info("Deleting all recipe plans for user ID: {} on date: {}", userId, date);
        recipePlanRepository.deleteByUserIdAndPlanDate(userId, date);
    }


    public ResponseEntity<ApiResponse<List<RecipePlanResponse>>> getPlansInDateRange(String userName, String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        User user = userService.getUserByUsername(userName);

        log.info("User found: {} with ID: {}", userName, user.getId());
        List<RecipePlanSimple> recipePlans = findByUserIdAndDateRange(user.getId(), start, end);
        if(recipePlans.isEmpty()) {
            log.info("No recipe plans found for user ID: {} between {} and {}", user.getId(), start, end);
            throw new RecipeNotFoundException("No recipe plans found in the specified date range.");
        }
        List<RecipePlanResponse> responseData = recipePlanMapper.toResponseSimple(recipePlans);

        ApiResponse<List<RecipePlanResponse>> response = new ApiResponse<>(true, "Recipe plans retrieved successfully", responseData);
        return ResponseEntity.ok(response);
    }


    @Transactional
    public ResponseEntity<ApiResponse<List<RecipePlanResponse>>> createBulkRecipePlans(String userName, List<BulkRecipePlanRequest> bulkPlanRequests) {
        User user = userService.getUserByUsername(userName);

        bulkPlanRequests.stream().map(BulkRecipePlanRequest::getRecipeId).filter(Objects::nonNull).forEach(recipeId -> {
            if (!recipeRepository.existsById(recipeId)) {
                throw new RecipeNotFoundException("Recipe not found with id: " + recipeId);
            }
        });

        log.info("User found: {} with ID: {}", userName, user.getId());
        log.info("Creating {} recipe plans in bulk for user ID: {}", bulkPlanRequests.size(), user.getId());

        List<RecipePlan> mappedPlans = recipePlanMapper.toEntity(bulkPlanRequests, user);

        List<RecipePlan> recipePlans = recipePlanRepository.saveAll(mappedPlans);
        log.info("Successfully created {} recipe plans for user ID: {}", recipePlans.size(), user.getId());
        return ResponseEntity.ok(ApiResponse.success("Recipe plans created successfully", recipePlanMapper.toResponse(recipePlans)));
    }

    /**
     * Delete multiple recipe plans in bulk
     *
     * @param userName The username of the authenticated user
     * @param planIds List of recipe plan IDs to delete
     * @return Response entity with success/failure status
     */
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteBulkRecipePlans(String userName, List<Long> planIds) {
        User user = userService.getUserByUsername(userName);

        log.info("User found: {} with ID: {}", userName, user.getId());
        log.info("Attempting to delete {} recipe plans in bulk for user ID: {}", planIds.size(), user.getId());

        // Delete plans that match both the plan IDs and the user ID
        recipePlanRepository.deleteByIdInAndUserId(planIds, user.getId());
        log.info("Successfully deleted recipe plans for user ID: {}", user.getId());

        return ResponseEntity.ok(ApiResponse.success("Recipe plans deleted successfully"));
    }


    @Transactional
    public ResponseEntity<ApiResponse<String>> toggleSavedStatusBulk(String userName, String startDate, String endDate, Boolean saved) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        User user = userService.getUserByUsername(userName);
        log.info("User found: {} with ID: {}", userName, user.getId());
        List<RecipePlanSimple> recipePlans = findByUserIdAndDateRange(user.getId(), start, end);
        if(recipePlans.isEmpty()) {
            log.info("No recipe plans found for user ID: {} between {} and {}", user.getId(), start, end);
            throw new RecipeNotFoundException("No recipe plans found in the specified date range.");
        }
        recipePlanRepository.updateSavedStatusBetweenDates(user.getId(), start, end, saved);
        return ResponseEntity.ok(ApiResponse.success("Recipe plans updated successfully"));
    }
}

