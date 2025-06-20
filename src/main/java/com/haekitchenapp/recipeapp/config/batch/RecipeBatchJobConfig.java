package com.haekitchenapp.recipeapp.config.batch;

import com.haekitchenapp.recipeapp.entity.RecipeUpdateFailure;
import com.haekitchenapp.recipeapp.exception.LlmApiException;
import com.haekitchenapp.recipeapp.model.response.togetherAi.LlmResponse;
import com.haekitchenapp.recipeapp.repository.RecipeUpdateFailureRepository;
import com.haekitchenapp.recipeapp.service.RecipeService;
import com.haekitchenapp.recipeapp.service.TogetherAiApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import com.haekitchenapp.recipeapp.entity.Recipe;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import jakarta.persistence.EntityManagerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.haekitchenapp.recipeapp.utility.BatchValidation.*;

@Slf4j
@Configuration
public class RecipeBatchJobConfig {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Value("${spring.batch.settings.chunk-size:10}")
    private int chunkSize;

    @Autowired
    private TogetherAiApi togetherAiApi;

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private RecipeUpdateFailureRepository recipeUpdateFailureRepository;

    @Bean
    public Job recipeUpdateJob() {
        return new JobBuilder("recipeUpdateJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(recipeUpdateStep())
                .build();
    }


    @Bean
    public Step recipeUpdateStep() {
        return new StepBuilder("recipeUpdateStep", jobRepository)
                .<Long, Recipe>chunk(chunkSize, transactionManager)
                .reader(recipeReader())
                .processor(recipeProcessor(null))
                .writer(this::updateRecipes)
                .faultTolerant()
                .skip(LlmApiException.class)
                .skipPolicy(new AlwaysSkipItemSkipPolicy()) // or custom one
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Long> recipeReader() {
        return new JpaPagingItemReaderBuilder<Long>()
                .name("recipeReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT r.id FROM Recipe r WHERE r.embedding IS NULL")
                .pageSize(chunkSize)
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<Long, Recipe> recipeProcessor(
            @Value("#{jobParameters['modValues']}") String modValues) {
        log.info("Recipe processor initialized with mod values: {}", modValues);
        Set<Integer> allowedMods = Arrays.stream(modValues.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
        return id -> {
            try{
                if (!allowedMods.contains((int) (id % 10))) {
                    log.info("Skipping recipe with ID {} due to mod value", id);
                    return null; // skip recipe
                }
                Duration startTime = Duration.ofMillis(System.currentTimeMillis());
                Recipe recipe = recipeService.getRecipeByIdWithIngredients(id);
                logTime("Fetched recipe with ID: " + id, startTime);
                String rewritten = validateRewrittenInstructions(callLLMRewrite(recipe.getInstructions()));
                logTime("Rewritten instructions for recipe with ID: " + id, startTime);
                String formattedTitle = validateTitle(callFormatTitle(recipe.getTitle()));
                logTime("Formatted title for recipe with ID: " + id, startTime);
                String summary = validateSummary(callLLMSummarize(rewritten));
                logTime("Summarized instructions for recipe with ID: " + id, startTime);
                String embedSummary = getFullSummary(recipe, summary);
                List<Double> embedding = validateEmbedding(callEmbeddingAPI(embedSummary));
                logTime("Generated embedding for recipe with ID: " + id, startTime);
                recipe.setTitle(formattedTitle);
                recipe.setInstructions(rewritten);
                recipe.setSummary(summary);
                recipe.setEmbedding(embedding);
                return recipe;
            } catch (LlmApiException e) {
                log.error("Error processing recipe {}: {}", id, e.getMessage());
                addFailedRecordToFailureRepository(id, e.getMessage());
            } catch (Exception e) {
                log.error("Unexpected error processing recipe {}: {}", id, e.getMessage());
                addFailedRecordToFailureRepository(id, e.getMessage());
            }
            return null;
        };
    }

    private void logTime(String s, Duration startTime) {
        Duration endTime = Duration.ofMillis(System.currentTimeMillis());
        log.info("{} in {} ms", s, endTime.minus(startTime).toMillis());
    }

    private String getFullSummary(Recipe recipe, String summary) {
        return "Title: " + recipe.getTitle()
                + "\n Ingredients: " + recipe.getIngredients().stream().map(recipeIngredient ->
                recipeIngredient.getIngredient().getName()).reduce((a, b) -> a + ", " + b).orElse("No ingredients")
                + "\n Instructions: " + summary;
    }

    private void updateRecipes(Chunk<? extends Recipe> recipes) {
        log.info("Updating {} recipes", recipes.getItems().size());
        for (Recipe recipe : recipes) {
            try {
                recipeService.updateRecipe(recipe);
            } catch (Exception e) {
                log.error("Failed to update recipe {}: {}", recipe.getId(), e.getMessage());
                addFailedRecordToFailureRepository(recipe.getId(), e.getMessage());
            }
        }
        log.info("Updated {} recipes", recipes.getItems().size());
    }

    private void addFailedRecordToFailureRepository(Long id, String message) {
        try {
            recipeUpdateFailureRepository.save(new RecipeUpdateFailure(id, message));
            log.info("Added failed record for recipe {} to failure repository", id);
        } catch (Exception e) {
            log.error("Failed to add record to failure repository for recipe {}: {}", id, e.getMessage());
        }
    }

    private String callLLMRewrite(String instructions) {
        try{
            LlmResponse response = togetherAiApi.callLLMRewrite(instructions);
            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                return response.getChoices().get(0).getMessage().getContent();
            } else {
                throw new LlmApiException("Context not returned in the response");
            }
        } catch (Exception e) {
            throw new LlmApiException("Error calling LLM rewrite: ", e);
        }
    }

    private String callFormatTitle(String title) {
        try{
            LlmResponse response = togetherAiApi.callLLMFormatTitle(title);
            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                return response.getChoices().get(0).getMessage().getContent().trim();
            } else {
                throw new LlmApiException("Context not returned in the response");
            }
        } catch (Exception e) {
            throw new LlmApiException("Error calling LLM rewrite: ", e);
        }
    }

    private String callLLMSummarize(String instructions) {
        try {
            LlmResponse response = togetherAiApi.callLLMSummarize(instructions);
            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                return response.getChoices().get(0).getMessage().getContent();
            } else {
                throw new LlmApiException("Summary not returned in the response");
            }
        } catch (Exception e) {
            throw new LlmApiException("Error calling LLM summarize: ", e);
        }
    }

    private List<Double> callEmbeddingAPI(String summary) {
        try {
            LlmResponse response = togetherAiApi.embed(summary);
            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                return response.getData().get(0).getEmbedding();
            } else {
                throw new LlmApiException("Embedding not returned in the response");
            }
        } catch (Exception e) {
            throw new LlmApiException("Error calling embedding API: ", e);
        }
    }
}

