package com.haekitchenapp.recipeapp.config.batch;

import com.haekitchenapp.recipeapp.client.ApiRetryConfig;
import com.haekitchenapp.recipeapp.entity.RecipeUpdateFailure;
import com.haekitchenapp.recipeapp.entity.RecipeStage;
import com.haekitchenapp.recipeapp.exception.LlmApiException;
import com.haekitchenapp.recipeapp.model.response.togetherAi.LlmResponse;
import com.haekitchenapp.recipeapp.repository.RecipeUpdateFailureRepository;
import com.haekitchenapp.recipeapp.service.RecipeService;
import com.haekitchenapp.recipeapp.service.RecipeStageService;
import com.haekitchenapp.recipeapp.service.TogetherAiApi;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import com.haekitchenapp.recipeapp.entity.Recipe;
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
    private RecipeStageService recipeStageService;

    @Autowired
    private RecipeUpdateFailureRepository recipeUpdateFailureRepository;

    @Autowired
    private ApiRetryConfig apiRetryConfig;

    @PersistenceContext
    private EntityManager entityManager;

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
                .<RecipeStage, Recipe>chunk(chunkSize, transactionManager)
                .reader(recipeReader())
                .processor(recipeProcessor())
                .writer(this::saveRecipes)
                .faultTolerant()
                .skip(LlmApiException.class)
                .skipPolicy(new AlwaysSkipItemSkipPolicy()) // or custom one
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    public JpaPagingItemReader<RecipeStage> recipeReader() {
        return new JpaPagingItemReaderBuilder<RecipeStage>()
                .name("recipeReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT r FROM RecipeStage r")
                .pageSize(chunkSize)
                .build();
    }

    @Bean
    public ItemProcessor<RecipeStage, Recipe> recipeProcessor() {
        return stage -> {
            try{
                Duration startTime = Duration.ofMillis(System.currentTimeMillis());
                Recipe recipe = stage.getRecipe();
                logTime("Fetched recipe with ID: " + stage.getId(), startTime);
                String formattedTitle = apiRetryConfig.retryTemplate(() ->
                        validateTitle(callFormatTitle(sanitizeTitle(recipe.getTitle()), stage.getId()))).trim();
                logTime("Formatted title for recipe with ID: " + stage, startTime);
                boolean shouldBeUpdated = apiRetryConfig.retryTemplate(() ->
                        getShouldInstructionsBeUpdated(getFullSummary(recipe, recipe.getInstructions()), stage.getId()));
                logTime("Checked if recipe with ID: " + stage + " should be updated", startTime);
                log.info("Recipe instructions with ID {} should be updated: {}", stage, shouldBeUpdated);
                String instructions = shouldBeUpdated ? apiRetryConfig.retryTemplate(() ->
                        validateRewrittenInstructions(callLLMRewrite(recipe.getInstructions(), stage.getId()),
                                recipe.getInstructions())).trim() : recipe.getInstructions();
                if(shouldBeUpdated) logTime("Rewritten instructions for recipe with ID: " + stage, startTime);
                String summary = apiRetryConfig.retryTemplate(() ->
                                validateSummary(callLLMSummarize(instructions, stage.getId()))).trim();
                logTime("Summarized instructions for recipe with ID: " + stage, startTime);
                String embedSummary = getFullSummary(recipe, summary);
                Double[] embedding = apiRetryConfig.retryTemplate(() ->
                                validateEmbedding(callEmbeddingAPI(embedSummary, stage.getId())));
                logTime("Generated embedding for recipe with ID: " + stage, startTime);
                recipe.setTitle(formattedTitle);
                recipe.setInstructions(instructions);
                recipe.setSummary(summary);
                recipe.setEmbedding(embedding);
                logTime("Processed recipe with ID: " + stage, startTime);
                return recipe;
            } catch (LlmApiException e) {
                log.error("Error processing recipe {}: {}", stage, e.getMessage());
                addFailedRecordToFailureRepository(stage.getId(), e.getMessage());
            } catch (Exception e) {
                log.error("Unexpected error processing recipe {}: {}", stage, e.getMessage());
                addFailedRecordToFailureRepository(stage.getId(), e.getMessage());
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

    private void saveRecipes(Chunk<? extends Recipe> recipes) {
        log.info("Updating {} recipes", recipes.getItems().size());
        for (Recipe recipe : recipes) {
            try {
                Long recipeId = recipe.getId();
                recipe.setId(null); // Ensure ID is null for update
                recipeService.saveRecipe(recipe);
                recipeStageService.deleteAndFlush(recipeId);
            } catch (Exception e) {
                log.error("Failed to update recipe {}: {}", recipe.getId(), e.getMessage());
                addFailedRecordToFailureRepository(recipe.getId(), e.getMessage());
            }
        }
        entityManager.clear();
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

    private boolean getShouldInstructionsBeUpdated(String recipeConstruct, Long recipeId){
        try{
            LlmResponse response = togetherAiApi.callIsBadRecipe(recipeConstruct, recipeId);
            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                String content = response.getChoices().get(0).getMessage().getContent();
                return content.toLowerCase().contains("false");
            } else {
                throw new LlmApiException("Validation response not returned in the response");
            }
        } catch (Exception e) {
            throw new LlmApiException("Error calling LLM validation: " + e.getMessage(), e);
        }
    }

    private String callLLMRewrite(String instructions, Long recipeId) {
        try{
            LlmResponse response = togetherAiApi.callLLMRewrite(instructions, recipeId);
            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                return response.getChoices().get(0).getMessage().getContent();
            } else {
                throw new LlmApiException("Context not returned in the response");
            }
        } catch (Exception e) {
            throw new LlmApiException("Error calling LLM rewrite: " + e.getMessage(), e);
        }
    }

    private String callFormatTitle(String title, Long recipeId) {
        try{
            LlmResponse response = togetherAiApi.callLLMFormatTitle(title, recipeId);
            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                return response.getChoices().get(0).getMessage().getContent().trim();
            } else {
                throw new LlmApiException("Context not returned in the response");
            }
        } catch (Exception e) {
            throw new LlmApiException("Error calling LLM rewrite: " + e.getMessage(), e);
        }
    }

    private String callLLMSummarize(String instructions, Long recipeId) {
        try {
            LlmResponse response = togetherAiApi.callLLMSummarize(instructions, recipeId);
            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                return response.getChoices().get(0).getMessage().getContent();
            } else {
                throw new LlmApiException("Summary not returned in the response");
            }
        } catch (Exception e) {
            throw new LlmApiException("Error calling LLM summarize: " + e.getMessage(), e);
        }
    }

    private Double[] callEmbeddingAPI(String summary, Long recipeId) {
        try {
            LlmResponse response = togetherAiApi.embed(summary, recipeId);
            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                return response.getData().get(0).getEmbedding();
            } else {
                throw new LlmApiException("Embedding not returned in the response");
            }
        } catch (Exception e) {
            throw new LlmApiException("Error calling embedding API: " + e.getMessage(), e);
        }
    }
}

