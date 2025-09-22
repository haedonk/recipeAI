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
import java.util.regex.Pattern;
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
        log.debug("Getting {} random recipe titles from database", numberOfTitles);

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

        log.debug("Successfully retrieved {} recipe titles from database", titles.size());
        return titles;
    }

    public ResponseEntity<ApiResponse<Long>> recipeChat(String query, Long userId) throws JsonProcessingException {
        log.info("Recipe chat request - User ID: {}, Query: {}", userId, query);
        RecipeAISkeleton response = openAiApi.buildRecipe(query);
        log.debug("Simple chat response: {}", response);
        Long recipeId = recipeService.createRecipe(response.toRecipeRequest(userId, null), true).getId();
        log.info("Recipe created with ID: {}", recipeId);
        return ResponseEntity.ok(ApiResponse.success("Recipe created successfully", recipeId));
    }

    public ResponseEntity<ApiResponse<Long>> recipeCleanUp(RecipeAISkeletonId query, Long userId) throws JsonProcessingException {
        log.info("Recipe clean up request - User ID: {}, Recipe ID to clean: {}", userId, query.getId());
        RecipeAISkeleton response = openAiApi.correctRecipe(query, query.getUserPrompt());
        log.debug("Clean up chat response: {}", response);
        Long recipeId = recipeService.createRecipe(response.toRecipeRequest(userId, query.getId()), true).getId();
        log.info("Recipe clean and created with ID: {}", recipeId);
        return ResponseEntity.ok(ApiResponse.success("Recipe created successfully", recipeId));
    }



    public ResponseEntity<ApiResponse<List<RecipeSimilarityDto>>> searchByAdvancedEmbeddingObject(RecipeSimilarityRequest query) {
        log.debug("Searching recipes by advanced embedding with query: {}", query);

        if(query.isPromptBased() && query.isValidFullRequest()) {
            throw new IllegalArgumentException("Must provide either full request fields or prompt, not both");
        } else if (!query.isPromptBased() && !query.isValidFullRequest()) {
            throw new IllegalArgumentException("Must provide at least one field in full request or a prompt");
        }

        if(query.getLimit() <= 0) {
            throw new IllegalArgumentException("Limit must be greater than 0");
        }

        String titleFilter = !query.isPromptBased() ? "%" + query.getTitle().trim().toLowerCase() + "%" : null;
        String queryString = query.isPromptBased() ? query.getPrompt() : query.toString();
        if(query.isPromptBased()) query.setExcludeIngredients("");

        return performAdvancedEmbeddingSearch(query, queryString, query.getLimit() * 2,
                titleFilter);
    }


    private ResponseEntity<ApiResponse<List<RecipeSimilarityDto>>> performAdvancedEmbeddingSearch(
            RecipeSimilarityRequest queryRequest, String embeddingQuery, int dbLimit, String titleFilter) {

        long startTime = System.currentTimeMillis();

        // Get embedding for similarity search
        long embeddingStartTime = System.currentTimeMillis();
        String embedding = getEmbeddingStringForSimilaritySearch(embeddingQuery);
        long embeddingEndTime = System.currentTimeMillis();
        log.debug("Embedding generation took {} ms", embeddingEndTime - embeddingStartTime);

        // Find recipes by embedding similarity
        long dbQueryStartTime = System.currentTimeMillis();
        List<RecipeSimilarityDto> recipes = titleFilter != null ?
            recipeRepository.findTopByEmbeddingSimilarityAndTitle(embedding, dbLimit, titleFilter) :
            recipeRepository.findTopByEmbeddingSimilarity(embedding, dbLimit);
        long dbQueryEndTime = System.currentTimeMillis();
        log.debug("Database similarity query took {} ms, found {} initial recipes", dbQueryEndTime - dbQueryStartTime, recipes.size());

        recipes = processSimilaritySearch(recipes, queryRequest, startTime);

        if (recipes.isEmpty()) {
            log.warn("No recipes found with advanced embedding for query: {}", queryRequest);
            return ResponseEntity.ok(ApiResponse.success("No recipes found with advanced embedding for query: " + queryRequest));
        }

        log.debug("Found {} recipes with advanced embedding for query: {}", recipes.size(), queryRequest);
        return ResponseEntity.ok(ApiResponse.success("Recipes with advanced embedding retrieved successfully", recipes));
    }

    private List<RecipeSimilarityDto> processSimilaritySearch(List<RecipeSimilarityDto> recipes, RecipeSimilarityRequest query,
                                                              long startTime){
        long limit = !query.isPromptBased() && query.getLimit() > 0 ? query.getLimit() : 20;

        // Calculate percentage similarity for each recipe
        calculatePercentageSimilarities(recipes);

        // Remove duplicate recipes by title
        recipes = removeDuplicateRecipes(recipes);

        // Get full recipe details asynchronously
        List<RecipeDetailsDto> recipeWithIngredients = fetchRecipeDetailsAsync(recipes);

        // Rank and sort recipes based on query criteria
        recipes = rankAndSortRecipes(recipes, recipeWithIngredients, query);

        // Apply final sorting and limiting
        recipes = appleFinalSortingAndLimiting(recipes, limit);

        logTotalSearchTime(startTime);
        log.info("Found {} recipes : {}", recipes.size(), recipes);
        return recipes;
    }

    private void calculatePercentageSimilarities(List<RecipeSimilarityDto> recipes) {
        recipes.forEach(recipe -> {
            double percentSimilarity = calculatePercentSimilarity(recipe.getSimilarity());
            recipe.setPercentSimilarity(percentSimilarity);
            log.debug("Recipe {} has {}% similarity", recipe.getTitle(), String.format("%.2f", percentSimilarity));
        });
    }

    private List<RecipeSimilarityDto> removeDuplicateRecipes(List<RecipeSimilarityDto> recipes) {
        long dedupeStartTime = System.currentTimeMillis();
        Set<String> titles = new HashSet<>();
        List<RecipeSimilarityDto> dedupedRecipes = recipes.stream()
                .filter(recipe -> {
                    if(titles.contains(recipe.getTitle().toLowerCase())) {
                        log.debug("Duplicate recipe title found: {}", recipe.getTitle());
                        return false; // Filter out duplicates
                    } else {
                        titles.add(recipe.getTitle().toLowerCase());
                        return true; // Keep unique recipes
                    }
                }).toList();
        long dedupeEndTime = System.currentTimeMillis();
        log.debug("Deduplication took {} ms, remaining recipes: {}", dedupeEndTime - dedupeStartTime, dedupedRecipes.size());
        return dedupedRecipes;
    }

    private List<RecipeDetailsDto> fetchRecipeDetailsAsync(List<RecipeSimilarityDto> recipes) {
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
        mapDetailsToSimilarityCuisines(recipes, recipeWithIngredients);
        log.debug("Fetching recipe details took {} ms for {} recipes", detailsEndTime - detailsStartTime, recipeWithIngredients.size());
        return recipeWithIngredients;
    }

    private void mapDetailsToSimilarityCuisines(List<RecipeSimilarityDto> recipes, List<RecipeDetailsDto> recipeWithIngredients) {
        Map<Long, List<String>> idToCuisines = recipeWithIngredients.stream()
                .collect(Collectors.toMap(RecipeDetailsDto::getId, RecipeDetailsDto::getCuisines));
        recipes.forEach(recipe -> recipe.setCuisines(idToCuisines.getOrDefault(recipe.getId(), List.of())));
    }

    private List<RecipeSimilarityDto> rankAndSortRecipes(List<RecipeSimilarityDto> recipes, List<RecipeDetailsDto> recipeWithIngredients, RecipeSimilarityRequest query) {
        long rankingStartTime = System.currentTimeMillis();
        List<RecipeSimilarityDto> rankedRecipes = rankAndSortRecipe(recipes, recipeWithIngredients, query);
        long rankingEndTime = System.currentTimeMillis();
        log.debug("Ranking and sorting took {} ms", rankingEndTime - rankingStartTime);
        return rankedRecipes;
    }

    private List<RecipeSimilarityDto> appleFinalSortingAndLimiting(List<RecipeSimilarityDto> recipes, long limit) {
        long sortingStartTime = System.currentTimeMillis();

        List<RecipeSimilarityDto> sortedRecipes = recipes.stream()
                .sorted(Comparator
                                .comparing(RecipeSimilarityDto::isExactTitleMatch).reversed()
                                .thenComparing(RecipeSimilarityDto::getTitleSimilarityRank, Comparator.reverseOrder())
                                .thenComparing(RecipeSimilarityDto::getSimilarityRank, Comparator.reverseOrder())
                                .thenComparing(RecipeSimilarityDto::getIncludesIngredientsCount, Comparator.reverseOrder())
                        // optional tie-breakers:
                        // .thenComparing(RecipeSimilarityDto::getCuisineMatchRank, Comparator.reverseOrder())
                        // .thenComparing(RecipeSimilarityDto::getId) // stable ordering
                )
                .limit(limit)
                .toList();

        long sortingEndTime = System.currentTimeMillis();
        log.debug("Final sorting and limiting took {} ms, final results: {}", sortingEndTime - sortingStartTime, sortedRecipes.size());
        return sortedRecipes;
    }

    private void logTotalSearchTime(long startTime) {
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("Total search by advanced embedding took {} ms", totalTime);
    }

    private List<RecipeSimilarityDto> rankAndSortRecipe(List<RecipeSimilarityDto> recipes, List<RecipeDetailsDto> recipeIngredients, RecipeSimilarityRequest query) {
        // Parse query words for similarity matching
        Set<String> parsedWordsNoArticles = parseQueryWords(query);
        log.info("Parsed words for ranking: {}", parsedWordsNoArticles);

        // Filter out recipes with excluded ingredients
        List<RecipeSimilarityDto> filteredRecipes = filterExcludedIngredients(recipes, recipeIngredients, query);

        // Score all recipes based on different criteria
        scoreAllRecipes(filteredRecipes, recipeIngredients, query, parsedWordsNoArticles);

        log.info("Recipes after ranking: {}", filteredRecipes);
        return filteredRecipes;
    }

    private Set<String> parseQueryWords(RecipeSimilarityRequest query) {
        String queryString = query.isPromptBased() ? query.getPrompt() :
            String.join(" ", query.getTitle(), query.getCuisine(),
                query.getIncludeIngredients(), query.getMealType(), query.getDetailLevel());

        return Arrays.stream(queryString.split("\\s+"))
                .map(String::toLowerCase)
                .filter(word -> !word.isBlank() && !word.matches("^(a|an|the|and|or|but)$"))
                .collect(Collectors.toSet());
    }

    private List<RecipeSimilarityDto> filterExcludedIngredients(List<RecipeSimilarityDto> recipes, List<RecipeDetailsDto> recipeIngredients, RecipeSimilarityRequest query) {
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

        return recipes.stream()
                .filter(recipe -> !idsToRemove.contains(recipe.getId()))
                .collect(Collectors.toList());
    }

    private void scoreAllRecipes(List<RecipeSimilarityDto> recipes, List<RecipeDetailsDto> recipeIngredients, RecipeSimilarityRequest query, Set<String> parsedWordsNoArticles) {
        log.info("Scoring recipe similarity for {} recipes", recipes.size());

        for (RecipeSimilarityDto recipe : recipes) {
            scoreRecipeTitleSimilarity(recipe, query);
            scoreRecipeWordSimilarity(recipe, query, parsedWordsNoArticles);
            scoreRecipeCuisineMatch(recipe, recipeIngredients, query);
            scoreIncludesIngredientsCount(recipe, recipeIngredients);
        }
    }

    private void scoreRecipeCuisineMatch(RecipeSimilarityDto recipe, List<RecipeDetailsDto> recipeIngredients, RecipeSimilarityRequest query) {
        if (query.getCuisine() == null || query.getCuisine().isBlank()) {
            recipe.setCuisineMatchRank(0);
            log.debug("No cuisine specified in query, setting cuisine match rank to 0 for recipe: {}", recipe.getTitle());
            return;
        }

        // Find the recipe details for this recipe
        RecipeDetailsDto recipeDetails = recipeIngredients.stream()
                .filter(details -> details.getId().equals(recipe.getId()))
                .findFirst()
                .orElse(null);

        if (recipeDetails == null || recipeDetails.getCuisines() == null || recipeDetails.getCuisines().isEmpty()) {
            recipe.setCuisineMatchRank(0);
            log.debug("No cuisines found for recipe: {}", recipe.getTitle());
            return;
        }

        String queryCuisine = query.getCuisine().toLowerCase().trim();
        boolean cuisineMatch = recipeDetails.getCuisines().stream()
                .anyMatch(cuisine -> cuisine.toLowerCase().contains(queryCuisine) ||
                                   queryCuisine.contains(cuisine.toLowerCase()));

        int cuisineRank = cuisineMatch ? 10 : 0; // Give high score for exact cuisine match
        recipe.setCuisineMatchRank(cuisineRank);

        log.debug("Recipe {} cuisine match: {} (query: {}, recipe cuisines: {})",
                 recipe.getTitle(), cuisineMatch, queryCuisine, recipeDetails.getCuisines());
    }

    private static final Pattern NON_WORD = Pattern.compile("[\\p{Punct}\\p{IsPunctuation}]");

    private List<String> tokenize(String s) {
        if (s == null) return List.of();
        String cleaned = NON_WORD.matcher(s.toLowerCase()).replaceAll(" ");
        return Arrays.stream(cleaned.trim().split("\\s+"))
                .filter(w -> !w.isBlank())
                .toList();
    }

    private void scoreRecipeTitleSimilarity(RecipeSimilarityDto recipe, RecipeSimilarityRequest query) {
        if (query.getTitle() == null || query.getTitle().isBlank() || recipe.getTitle() == null) {
            recipe.setTitleSimilarityRank(0);
            recipe.setExactTitleMatch(false);
            recipe.setPrefixTitleMatch(false);
            return;
        }

        String recipeTitleRaw = recipe.getTitle().trim();
        String queryTitleRaw  = query.getTitle().trim();

        // Strong precedence signals
        boolean exact = recipeTitleRaw.equalsIgnoreCase(queryTitleRaw);
        boolean prefix = recipeTitleRaw.toLowerCase().startsWith(queryTitleRaw.toLowerCase());

        // Token-based similarity (Jaccard)
        Set<String> r = new HashSet<>(tokenize(recipeTitleRaw));
        Set<String> q = new HashSet<>(tokenize(queryTitleRaw));
        if (r.isEmpty() || q.isEmpty()) {
            recipe.setTitleSimilarityRank(exact ? 100 : prefix ? 85 : 0);
            recipe.setExactTitleMatch(exact);
            recipe.setPrefixTitleMatch(prefix);
            return;
        }

        Set<String> inter = new HashSet<>(r); inter.retainAll(q);
        Set<String> union = new HashSet<>(r); union.addAll(q);

        double jaccard = (double) inter.size() / union.size();
        int base = (int) Math.round(jaccard * 80); // 0..80 from Jaccard

        int score = base
                + (prefix ? 5 : 0)   // small bump for startsWith
                + (exact ? 20 : 0);  // big bump for exact

        score = Math.min(score, 100);

        recipe.setTitleSimilarityRank(score);
        recipe.setExactTitleMatch(exact);
        recipe.setPrefixTitleMatch(prefix);
    }


    private void scoreIncludesIngredientsCount(RecipeSimilarityDto recipe, List<RecipeDetailsDto> recipeDetailsList) {
        long includesIngredientsCount = recipeDetailsList.stream()
                .filter(recipeDetail -> recipeDetail.getId().equals(recipe.getId()))
                .mapToLong(recipeDetail -> recipeDetail.getIngredients().size())
                .peek(count -> log.debug("Includes ingredients count for recipe {}: {}", recipe.getTitle(), count))
                .sum();
        recipe.setIncludesIngredientsCount((int) includesIngredientsCount);
        log.debug("Recipe includes ingredients count for {}: {}", recipe.getTitle(), includesIngredientsCount);
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
                .peek(word -> log.debug("Matching word: {}", word))
                .count();
        recipe.setSimilarityRank((int) matchingWordsCount);
        log.debug("Recipe {} has {} matching words with query, similarity rank: {}", recipe.getTitle(), matchingWordsCount, recipe.getSimilarityRank());
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
