package com.haekitchenapp.recipeapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.haekitchenapp.recipeapp.exception.EmbedFailureException;
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeSimilarityRequest;
import com.haekitchenapp.recipeapp.model.response.ApiResponse;
import com.haekitchenapp.recipeapp.model.response.recipe.*;
import com.haekitchenapp.recipeapp.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecipeAIService {

    private final RecipeRepository recipeRepository;
    private final RecipeService recipeService;
    private final TogetherAiApi togetherAiApi;
    private final OpenAiApi openAiApi;


    public ResponseEntity<ApiResponse<List<RecipeTitleDto>>> generateRandomRecipeTitles(Integer numberOfTitles) {
        if (numberOfTitles == null || numberOfTitles <= 0) {
            numberOfTitles = 10; // Default to 10 titles if not specified or invalid
        } else if (numberOfTitles > 50) {
            numberOfTitles = 200; // Cap at 50 titles to avoid excessive load
        }
        return ResponseEntity.ok(ApiResponse.success("RecipeStage retrieved successfully", getRandomRecipeTitles(numberOfTitles)));
    }

    public List<RecipeTitleDto> getRandomRecipeTitles(int numberOfTitles) {
        log.info("Getting {} random recipe titles from database", numberOfTitles);

        // Get a larger set and then randomly select from them
        Pageable pageable = PageRequest.of(0, numberOfTitles * 3); // Get 3x more to have variety
        List<RecipeTitleDto> recipeTitles = recipeRepository.findTitlesByTitleContainingIgnoreCase("", pageable);

        if (recipeTitles.isEmpty()) {
            log.warn("No recipes found in database");
            return List.of();
        }

        // Shuffle and take the requested number
        Collections.shuffle(recipeTitles);
        List<RecipeTitleDto> titles = recipeTitles.stream()
                .limit(numberOfTitles)
                .toList();

        log.info("Successfully retrieved {} recipe titles from database", titles.size());
        return titles;
    }

    public ResponseEntity<ApiResponse<Long>> recipeChat(String query, Long userId) throws JsonProcessingException {
        log.info("Recipe chat request - User ID: {}, Query: {}", userId, query);
        RecipeAISkeleton response = openAiApi.buildRecipe(query);
        log.info("Simple chat response: {}", response);
        Long recipeId = recipeService.createRecipe(response.toRecipeRequest(userId, null), true).getId();
        log.info("Recipe created with ID: {}", recipeId);
        return ResponseEntity.ok(ApiResponse.success("Recipe created successfully", recipeId));
    }

    public ResponseEntity<ApiResponse<Long>> recipeCleanUp(RecipeAISkeletonId query, Long userId) throws JsonProcessingException {
        log.info("Recipe clean up request - User ID: {}, Recipe ID to clean: {}", userId, query.getId());
        RecipeAISkeleton response = openAiApi.correctRecipe(query, query.getUserPrompt());
        log.info("Clean up chat response: {}", response);
        Long recipeId = recipeService.createRecipe(response.toRecipeRequest(userId, query.getId()), true).getId();
        log.info("Recipe created with ID: {}", recipeId);
        return ResponseEntity.ok(ApiResponse.success("Recipe created successfully", recipeId));
    }



    public ResponseEntity<ApiResponse<List<RecipeSimilarityDto>>> searchByAdvancedEmbeddingObject(RecipeSimilarityRequest query) {
        log.info("Searching recipes by advanced embedding with query: {}", query);
        long startTime = System.currentTimeMillis();

        if(query.getLimit() <= 0) {
            throw new IllegalArgumentException("Limit must be greater than 0");
        }

        // Get embedding for similarity search
        long embeddingStartTime = System.currentTimeMillis();
        String embedding = getEmbeddingStringForSimilaritySearch(query.toString());
        long embeddingEndTime = System.currentTimeMillis();
        log.info("Embedding generation took {} ms", embeddingEndTime - embeddingStartTime);

        // Find recipes by embedding similarity
        long dbQueryStartTime = System.currentTimeMillis();
        List<RecipeSimilarityDto> recipes = recipeRepository.findTopByEmbeddingSimilarityAndTitle(embedding, query.getLimit() + 30,
                "%" + query.getTitle().toLowerCase() + "%");
        long dbQueryEndTime = System.currentTimeMillis();
        log.info("Database similarity query took {} ms, found {} initial recipes", dbQueryEndTime - dbQueryStartTime, recipes.size());

        recipes = processSimilaritySearch(recipes, query, startTime);

        if (recipes.isEmpty()) {
            log.warn("No recipes found with advanced embedding for query: {}", query);
            return ResponseEntity.ok(ApiResponse.success("No recipes found with advanced embedding for query: " + query));
        }
        log.info("Found {} recipes with advanced embedding for query: {}", recipes.size(), query);
        return ResponseEntity.ok(ApiResponse.success("Recipes with advanced embedding retrieved successfully", recipes));
    }


    public ResponseEntity<ApiResponse<List<RecipeSimilarityDto>>> searchByAdvancedEmbedding(String query) {
        log.info("Searching recipes by advanced embedding with query: {}", query);
        long startTime = System.currentTimeMillis();

        if(query.isEmpty()) {
            throw new IllegalArgumentException("query must be greater than 0");
        }

        // Get embedding for similarity search
        long embeddingStartTime = System.currentTimeMillis();
        String embedding = getEmbeddingStringForSimilaritySearch(query);
        long embeddingEndTime = System.currentTimeMillis();
        log.info("Embedding generation took {} ms", embeddingEndTime - embeddingStartTime);

        // Find recipes by embedding similarity
        long dbQueryStartTime = System.currentTimeMillis();
        List<RecipeSimilarityDto> recipes = recipeRepository.findTopByEmbeddingSimilarity(embedding, 30);
        long dbQueryEndTime = System.currentTimeMillis();
        log.info("Database similarity query took {} ms, found {} initial recipes", dbQueryEndTime - dbQueryStartTime, recipes.size());

        recipes = processSimilaritySearch(recipes, new RecipeSimilarityRequest(query), startTime);

        if (recipes.isEmpty()) {
            log.warn("No recipes found with advanced embedding for query: {}", query);
            return ResponseEntity.ok(ApiResponse.success("No recipes found with advanced embedding for query: " + query));
        }

        log.info("Found {} recipes with advanced embedding for query: {}", recipes.size(), query);
        return ResponseEntity.ok(ApiResponse.success("Recipes with advanced embedding retrieved successfully", recipes));
    }

    private List<RecipeSimilarityDto> processSimilaritySearch(List<RecipeSimilarityDto> recipes, RecipeSimilarityRequest query,
                                                              long startTime){
        long limit = !query.isPromptBased() && query.getLimit() > 0 ? query.getLimit() : 20;

        // Calculate percentage similarity for each recipe
        recipes.forEach(recipe -> {
            double percentSimilarity = calculatePercentSimilarity(recipe.getSimilarity());
            recipe.setPercentSimilarity(percentSimilarity);
            log.info("Recipe {} has {}% similarity", recipe.getTitle(), String.format("%.2f", percentSimilarity));
        });

        long dedupeStartTime = System.currentTimeMillis();
        Set<String> titles = new HashSet<>();
        recipes = recipes.stream()
                .filter(recipe -> {
                    if(titles.contains(recipe.getTitle().toLowerCase())) {
                        log.info("Duplicate recipe title found: {}", recipe.getTitle());
                        return false; // Filter out duplicates
                    } else {
                        titles.add(recipe.getTitle().toLowerCase());
                        return true; // Keep unique recipes
                    }
                }).toList();
        long dedupeEndTime = System.currentTimeMillis();
        log.info("Deduplication took {} ms, remaining recipes: {}", dedupeEndTime - dedupeStartTime, recipes.size());

        // Get full recipe details
        long detailsStartTime = System.currentTimeMillis();
        List<CompletableFuture<RecipeDetailsDto>> futures = recipes.stream()
                .map(recipe -> CompletableFuture.supplyAsync(() -> recipeService.getRecipeDetails(recipe.getId())))
                .toList();
        List<RecipeDetailsDto> recipeWithIngredients = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .toList())
                .join();
        long detailsEndTime = System.currentTimeMillis();
        log.info("Fetching recipe details took {} ms for {} recipes", detailsEndTime - detailsStartTime, recipeWithIngredients.size());

        long rankingStartTime = System.currentTimeMillis();
        // FIXED: Update recipes with the filtered and ranked list
        recipes = rankAndSortRecipe(recipes, recipeWithIngredients, query);
        long rankingEndTime = System.currentTimeMillis();
        log.info("Ranking and sorting took {} ms", rankingEndTime - rankingStartTime);

        // Final sorting and limiting
        long sortingStartTime = System.currentTimeMillis();
        recipes = recipes.stream()
                .sorted(Comparator
                        .comparing(RecipeSimilarityDto::getTitleSimilarityRank).reversed()
                        .thenComparing(RecipeSimilarityDto::getSimilarityRank, Comparator.reverseOrder())
                        .thenComparing(RecipeSimilarityDto::getIncludesIngredientsCount, Comparator.reverseOrder()))
                .limit(limit)
                .collect(Collectors.toList());
        long sortingEndTime = System.currentTimeMillis();
        log.info("Final sorting and limiting took {} ms, final result count: {}", sortingEndTime - sortingStartTime, recipes.size());

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("Total search by advanced embedding took {} ms", totalTime);

        log.info("Found {} recipes : {}", recipes.size(), recipes);
        return recipes;
    }

    private List<RecipeSimilarityDto> rankAndSortRecipe(List<RecipeSimilarityDto> recipes, List<RecipeDetailsDto> recipeIngredients, RecipeSimilarityRequest query) {
        String queryString = query.isPromptBased() ? query.getPrompt() : String.join(" ", query.getTitle(), query.getCuisine(),
                query.getIncludeIngredients(), query.getMealType(), query.getDetailLevel());
        Set<String> parsedWordsNoArticles = Arrays.stream(queryString.split("\\s+"))
                .map(String::toLowerCase)
                .filter(word -> !word.isBlank() && !word.matches("^(a|an|the|and|or|but)$"))
                .collect(Collectors.toSet());
        log.info("Parsed words for ranking: {}", parsedWordsNoArticles);

        log.info("Removing recipes with exclude ingredients: {}", query.getExcludeIngredients());
        Set<String> excludeIngredients = Arrays.stream(query.getExcludeIngredients().split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        List<Long> idsToRemove = recipeIngredients.stream()
                .filter(recipe -> recipe.getIngredients().stream()
                        .anyMatch(ingredient -> excludeIngredients.contains(ingredient.toLowerCase())))
                .map(RecipeDetailsDto::getId)
                .collect(Collectors.toList());

        log.info("Removing recipes with IDs: {}", idsToRemove);

        // FIXED: Create a filtered list and use that for subsequent processing
        List<RecipeSimilarityDto> filteredRecipes = recipes.stream()
                .filter(recipe -> !idsToRemove.contains(recipe.getId()))
                .collect(Collectors.toList());

        log.info("Scoring recipe similarity for {} recipes", filteredRecipes.size());
        for (RecipeSimilarityDto recipe : filteredRecipes) {
            scoreRecipeTitleSimilarity(recipe, query);
            scoreRecipeWordSimilarity(recipe, query, parsedWordsNoArticles);
            scoreIncludesIngredientsCount(recipe, recipeIngredients);
        }

        log.info("Recipes after ranking: {}", filteredRecipes);

        // Return the filtered and ranked list
        return filteredRecipes;
    }

    private void scoreRecipeTitleSimilarity(RecipeSimilarityDto recipe, RecipeSimilarityRequest query) {
        if(query.getTitle() == null || query.getTitle().isBlank()) {
            recipe.setTitleSimilarityRank(0);
            return;
        }
        String recipeTitle = recipe.getTitle().toLowerCase().trim();
        String queryTitle = query.getTitle().toLowerCase().trim();
        long titleSimilarityCount = Arrays.stream(recipeTitle.split("\\s+"))
                .filter(queryTitle::contains)
                .peek(word -> log.info("Matching word in title: {}", word))
                .count();
        if(recipeTitle.split("\\s+").length == queryTitle.split("\\s+").length) {
            titleSimilarityCount += 1;
        }
        recipe.setTitleSimilarityRank((int) titleSimilarityCount);
        log.info("Recipe {} has a title similarity score of {}", recipe.getTitle(), recipe.getSimilarity());
    }

    private void scoreIncludesIngredientsCount(RecipeSimilarityDto recipe, List<RecipeDetailsDto> recipeDetailsList) {
        long includesIngredientsCount = recipeDetailsList.stream()
                .filter(recipeDetail -> recipeDetail.getId().equals(recipe.getId()))
                .mapToLong(recipeDetail -> recipeDetail.getIngredients().size())
                .peek(count -> log.info("Includes ingredients count for recipe {}: {}", recipe.getTitle(), count))
                .sum();
        recipe.setIncludesIngredientsCount((int) includesIngredientsCount);
        log.info("Recipe includes ingredients count for {}: {}", recipe.getTitle(), includesIngredientsCount);
    }

    private void scoreRecipeWordSimilarity(RecipeSimilarityDto recipe, RecipeSimilarityRequest query,
                                           Set<String> parsedWordsNoArticles) {
        String recipeString = String.join(" ", recipe.getTitle(), recipe.getSummary());
        Set<String> recipeWordsNoArticles = Arrays.stream(recipeString.split("\\s+"))
                .map(String::toLowerCase)
                .filter(word -> !word.isBlank() && !word.matches("^(a|an|the|and|or|but)$"))
                .collect(Collectors.toSet());
        long matchingWordsCount = parsedWordsNoArticles.stream()
                .filter(recipeWordsNoArticles::contains)
                .peek(word -> log.info("Matching word: {}", word))
                .count();
        recipe.setSimilarityRank((int) matchingWordsCount);
        log.info("Recipe {} has {} matching words with query, similarity rank: {}", recipe.getTitle(), matchingWordsCount, recipe.getSimilarityRank());
    }


    public String getEmbeddingStringForSimilaritySearch(String query) {
        log.info("Getting embedding for query: {}", query);
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query must not be null or empty");
        }
        Double[] embedding = togetherAiApi.embed(query);
        if (embedding == null || embedding.length == 0) {
            log.warn("No embedding returned for query: {}", query);
            throw new EmbedFailureException("No embedding found for query: " + query);
        }
        log.info("Embedding retrieved successfully for query: {}", query);
        return Arrays.stream(embedding)
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }

    /**
     * Converts cosine similarity to percentage similarity
     * Cosine similarity ranges from -1 to 1, where:
     * - 1 = identical (100% similar)
     * - 0 = orthogonal (50% similar)
     * - -1 = opposite (0% similar)
     *
     * @param cosineSimilarity The cosine similarity value in range [-1, 1]
     * @return The percentage similarity in range [0, 100]
     */
    private double calculatePercentSimilarity(double cosineSimilarity) {
        // Convert from [-1, 1] range to [0, 100] percentage
        return ((cosineSimilarity + 1) / 2) * 100;
    }

    /**
     * Alternative method for normalized cosine similarity (0 to 1 range)
     * Use this if your cosine similarity is already normalized to [0, 1]
     *
     * @param normalizedCosineSimilarity The normalized cosine similarity in range [0, 1]
     * @return The percentage similarity in range [0, 100]
     */
    private double calculatePercentSimilarityNormalized(double normalizedCosineSimilarity) {
        return normalizedCosineSimilarity * 100;
    }


}
