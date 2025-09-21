package com.haekitchenapp.recipeapp.service;

import com.haekitchenapp.recipeapp.entity.LlmQueryLog;
import com.haekitchenapp.recipeapp.model.request.togetherAi.RoleContent;
import com.haekitchenapp.recipeapp.model.response.togetherAi.LlmResponse;
import com.haekitchenapp.recipeapp.repository.LlmQueryLogRepository;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.completions.CompletionUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LlmLoggingService {

    private final LlmQueryLogRepository llmQueryLogRepository;
    private final LlmPricingService llmPricingService;

    /**
     * Saves a log of an LLM query to the database and calculates pricing
     *
     * @param model The model used for the query
     * @param systemPrompt The system prompt used
     * @param userPrompt The user prompt used
     * @param completion The completion response from the LLM
     */
    public void saveQueryLog(String model, String systemPrompt, String userPrompt, ChatCompletion completion) {
        try {
            String text = completion.choices().isEmpty() ? "" :
                    completion.choices().get(0).message().content().orElse("");

            long total = 0L;
            long promptTokens = 0L;
            long completionTokens = 0L;
            long reasoningTokens = 0L;

            if(completion.usage().isPresent()){
                CompletionUsage usage = completion.usage().get();
                total = usage.totalTokens();
                promptTokens = usage.promptTokens();
                completionTokens = usage.completionTokens();
                reasoningTokens = usage.completionTokensDetails().isPresent() &&
                                  usage.completionTokensDetails().get().reasoningTokens().isPresent() ?
                                  usage.completionTokensDetails().get().reasoningTokens().get() : 0L;
            } else {
                log.warn("No usage info in completion response");
            }

            // Create and save the LlmQueryLog with the correct constructor signature
            LlmQueryLog queryLog = new LlmQueryLog(
                    completion.id(),      // id
                    model,                // model
                    userPrompt,           // userPrompt
                    systemPrompt,         // systemPrompt
                    text,                 // response
                    (int) total,          // totalTokens
                    (int) completionTokens, // responseTokens
                    (int) promptTokens,   // promptTokens
                    (int) reasoningTokens, // reasoningTokens
                    0.0,                  // inputCost
                    0.0,                  // outputCost
                    0.0                   // totalCost
            );

            // Save the log to get an ID
            LlmQueryLog savedLog = llmQueryLogRepository.save(queryLog);

            log.info("Saved LLM query log with ID: {}", savedLog.getId());

            // Calculate and update the pricing information
            llmPricingService.calculateAndUpdateQueryPrice(savedLog.getId());

            log.debug("Saved query log with ID: {} and calculated pricing", savedLog.getId());
        } catch (Exception e) {
            log.warn("Failed to save LLM log: {}", e.getMessage(), e);
        }
    }

    /**
     * Saves a log of an LLM query to the database and calculates pricing
     *
     * @param model The model used for the query
     * @param systemPrompt The system prompt used
     * @param messages The list of role content messages
     * @param completion The completion response from the LLM
     */
    public void saveQueryLog(String model, String systemPrompt, List<RoleContent> messages, ChatCompletion completion) {
        String userPrompt = extractUserPrompt(messages);
        saveQueryLog(model, systemPrompt, userPrompt, completion);
    }

    /**
     * Extract the user prompt from a list of role content messages
     */
    private String extractUserPrompt(List<RoleContent> messages) {
        return messages.stream()
                .filter(RoleContent::isUserRole)
                .map(RoleContent::getContent)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    /**
     * Saves a log of a Together AI LLM query to the database and calculates pricing
     *
     * @param model The model used for the query
     * @param prompt The prompt used (will be treated as user prompt)
     * @param response The LlmResponse from Together AI
     * @param recipeId The associated recipe ID
     */
    public void saveTogetherAiQueryLog(String model, String prompt, LlmResponse response, Long recipeId) {
        try {
            String llmResponse = response.getChoices() == null ?
                    Arrays.toString(response.getData().get(0).getEmbedding()) :
                    response.getChoices().get(0).getMessage().getContent();

            // For TogetherAi, we're typically using prompt as the user prompt
            // and the system prompt is set separately in the LLMRequest
            String userPrompt = prompt;
            String systemPrompt = "";  // System prompt is usually set in the LLMRequest

            // Create and save the LlmQueryLog with the correct constructor signature
            LlmQueryLog queryLog = new LlmQueryLog(
                    response.getId(),                                // id
                    model,                                           // model
                    userPrompt,                                      // userPrompt
                    systemPrompt,                                    // systemPrompt
                    llmResponse,                                     // response
                    response.getUsage().getTotalTokens().intValue(), // totalTokens
                    response.getUsage().getCompletionTokens().intValue(), // responseTokens
                    response.getUsage().getPromptTokens().intValue(),  // promptTokens
                    0,                                               // reasoningTokens
                    0.0,                                             // inputCost
                    0.0,                                             // outputCost
                    0.0,                                             // totalCost
                    recipeId                                         // recipeId
            );

            // Save the log to get an ID
            LlmQueryLog savedLog = llmQueryLogRepository.save(queryLog);

            log.info("Saved Together AI LLM query log with ID: {}", savedLog.getId());

            // Calculate and update the pricing information
            llmPricingService.calculateAndUpdateQueryPrice(savedLog.getId());

            log.debug("Saved Together AI query log with ID: {} and calculated pricing", savedLog.getId());
        } catch (Exception e) {
            log.warn("Failed to save Together AI LLM log: {}", e.getMessage(), e);
        }
    }
}
