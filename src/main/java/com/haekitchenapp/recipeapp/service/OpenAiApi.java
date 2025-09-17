package com.haekitchenapp.recipeapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haekitchenapp.recipeapp.config.api.OpenAiConfig;
import com.haekitchenapp.recipeapp.entity.LlmQueryLog;
import com.haekitchenapp.recipeapp.model.request.togetherAi.RoleContent;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeAISkeleton;
import com.haekitchenapp.recipeapp.repository.LlmQueryLogRepository;
import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.*;
import com.openai.models.completions.CompletionUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.haekitchenapp.recipeapp.config.constants.Constant.CORRECT_INGREDIENTS_PROMPT;
import static com.haekitchenapp.recipeapp.config.constants.Constant.RECIPE_PARSER_GENERATOR_PROMPT;

@Service
@Slf4j
@RequiredArgsConstructor
public class OpenAiApi {

    private final OpenAIClient openAIClient;
    private final OpenAiConfig config;
    private final LlmQueryLogRepository llmQueryLogRepository;
    private final LlmPricingService llmPricingService;
    private final UnitService unitService;

    // ---- Public API (same shape as before, now returns the SDK object) ----

    public ChatCompletion chat(String systemPrompt, List<RoleContent> messages) {
        ChatCompletionCreateParams params = buildParams(config.getChatModel(), systemPrompt, messages, false);
        log.info("OpenAI SDK Chat.create: model={}, messages={}", config.getChatModel(), messages.size());
        ChatCompletion completion = openAIClient.chat().completions().create(params);
        // optional: persist a short log like before
        saveQueryLog(config.getChatModel(), summarizePrompt(systemPrompt, messages), completion);
        return completion;
    }

    public RecipeAISkeleton buildRecipe(String systemPrompt, List<RoleContent> messages) throws JsonProcessingException {
        ChatCompletionCreateParams params = buildParams(config.getChatModel(), systemPrompt, messages, true);
        log.info("OpenAI SDK Chat.create: model={}, messages={}", config.getChatModel(), messages.size());
        ChatCompletion completion = openAIClient.chat().completions().create(params);
        log.info("Completion received: {}", completion);
        // optional: persist a short log like before
        saveQueryLog(config.getChatModel(), summarizePrompt(systemPrompt, messages), completion);
        return getRecipeFromCompletion(completion);
    }

    public RecipeAISkeleton correctRecipe(String systemPrompt, List<RoleContent> messages) throws JsonProcessingException {
        ChatCompletionCreateParams params = buildParams(config.getChatModel(), systemPrompt, messages, true);
        log.info("OpenAI SDK Chat.create: model={}, messages={}", config.getChatModel(), messages.size());
        ChatCompletion completion = openAIClient.chat().completions().create(params);
        log.info("Completion received: {}", completion);
        // optional: persist a short log like before
        saveQueryLog(config.getChatModel(), summarizePrompt(systemPrompt, messages), completion);
        return getRecipeFromCompletion(completion);
    }

    private RecipeAISkeleton getRecipeFromCompletion(ChatCompletion completion) throws JsonProcessingException {
        if (completion.choices().isEmpty()) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        var message = completion.choices().get(0).message();
        return mapper.readValue(message.content().orElse(""), RecipeAISkeleton.class);
    }

    public RecipeAISkeleton buildRecipe(String systemPrompt, String userMessage) throws JsonProcessingException {
        List<RoleContent> messages = new ArrayList<>();
        messages.add(RoleContent.getUserRole(userMessage));
        return buildRecipe(systemPrompt, messages);
    }

    public RecipeAISkeleton buildRecipe(String userMessage) throws JsonProcessingException {
        List<RoleContent> messages = new ArrayList<>();
        messages.add(RoleContent.getUserRole(userMessage));
        return buildRecipe(getRecipePrompt(RECIPE_PARSER_GENERATOR_PROMPT), messages);
    }

    public RecipeAISkeleton correctRecipe(RecipeAISkeleton userMessage, String userPrompt) throws JsonProcessingException {
        List<RoleContent> messages = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        messages.add(RoleContent.getUserRole(mapper.writeValueAsString(userMessage)));
        if(userPrompt != null && !userPrompt.isBlank()){
            messages.add(RoleContent.getUserRole(userPrompt));
        }
        return correctRecipe(getRecipePrompt(CORRECT_INGREDIENTS_PROMPT), messages);
    }

    private String getRecipePrompt(String sysPrompt) {
        return String.format(sysPrompt, unitService.getAllUnitsMap().values().stream().toList());
    }

    public ChatCompletion chat(String systemPrompt, String userMessage) {
        List<RoleContent> messages = new ArrayList<>();
        messages.add(RoleContent.getUserRole(userMessage));
        return chat(systemPrompt, messages);
    }

    public ChatCompletion chat(List<RoleContent> messages) {
        ChatCompletionCreateParams params = buildParams(config.getChatModel(), null, messages, false);
        log.info("OpenAI SDK Chat.create: model={}, messages={}", config.getChatModel(), messages.size());
        ChatCompletion completion = openAIClient.chat().completions().create(params);
        saveQueryLog(config.getChatModel(), summarizePrompt(null, messages), completion);
        return completion;
    }

    public ChatCompletion chatWithModel(String model, String systemPrompt, List<RoleContent> messages) {
        ChatCompletionCreateParams params = buildParams(model, systemPrompt, messages, false);
        log.info("OpenAI SDK Chat.create: model={}, messages={}", model, messages.size());
        ChatCompletion completion = openAIClient.chat().completions().create(params);
        saveQueryLog(model, summarizePrompt(systemPrompt, messages), completion);
        return completion;
    }

    // ---- Builders / helpers ----

    private ChatCompletionCreateParams buildParams(String modelId, String systemPrompt, List<RoleContent> roleContents, boolean useRecipeResponseFormat) {
        log.info("Building ChatCompletionCreateParams for model: {}, systemPrompt: {}, messages: {}",
                modelId, (systemPrompt != null ? "[present]" : "[null]"), roleContents.size());

        String userMessage = roleContents.stream().filter(RoleContent::isUserRole).map(RoleContent::getContent).reduce((a, b) -> a + "\n" + b).orElse(null);

        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
                .model(resolveModel(modelId));

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            builder.addMessage(ChatCompletionSystemMessageParam.builder()
                    .content(systemPrompt)
                    .build());
        }

        if (userMessage != null && !userMessage.isBlank()) {
            builder.addMessage(ChatCompletionUserMessageParam.builder()
                    .content(userMessage)
                    .build());
        }

        if(useRecipeResponseFormat){
            builder.responseFormat(RecipeAISkeleton.class);
        }

        builder.maxCompletionTokens(10000);

        if (supportsAdjustableTemperature(modelId)) {
            builder.temperature(0.2);
        }

        return builder.build();
    }

    private ChatModel resolveModel(String modelId) {
        // Use constants when you hard-code a known model, otherwise .of(...)
        return ChatModel.of(modelId); // e.g., "gpt-5-nano"
    }

    private boolean supportsAdjustableTemperature(String model) {
        if (model == null) return true;
        String m = model.toLowerCase();
        return !(m.startsWith("gpt-5") || m.startsWith("o1") || m.startsWith("o3"));
    }

    private String summarizePrompt(String systemPrompt, List<RoleContent> messages) {
        String head = (systemPrompt != null ? ("SYS: " + systemPrompt + " | ") : "");
        String first = messages.isEmpty() ? "" : messages.get(0).getContent();
        String txt = (head + first);
        return txt.length() > 100 ? txt.substring(0, 100) + "..." : txt;
    }

    private void saveQueryLog(String model, String prompt, ChatCompletion completion) {
        try {
            String text = completion.choices().isEmpty() ? "" :
                    completion.choices().get(0).message().content().orElse("");
            if (text.length() > 100) text = text.substring(0, 100) + "...";
            long total = 0L;
            long promptTokens = 0L;
            long completionTokens = 0L;
            long reasoningTokens = 0L;
            if(completion.usage().isPresent()){
                CompletionUsage usage = completion.usage().get();
                total = usage.totalTokens();
                promptTokens = usage.promptTokens();
                completionTokens = usage.completionTokens();
                reasoningTokens = usage.completionTokensDetails().isPresent() && usage.completionTokensDetails().get().reasoningTokens().isPresent() ?
                        usage.completionTokensDetails().get().reasoningTokens().get() : 0L;
            } else {
                log.warn("No usage info in completion response");
            }

            // Create and save the LlmQueryLog
            LlmQueryLog queryLog = new LlmQueryLog(
                    completion.id(),
                    model,
                    prompt,
                    text,
                    (int) total,
                    (int) promptTokens,
                    (int) completionTokens,
                    (int) reasoningTokens,
                    0L
            );

            // Save the log to get an ID
            LlmQueryLog savedLog = llmQueryLogRepository.save(queryLog);

            log.info("Saved LLM query log with ID: {}", savedLog.getId());

            // Calculate and update the pricing information
            llmPricingService.calculateAndUpdateQueryPrice(savedLog.getId());

            log.debug("Saved query log with ID: {} and calculated pricing", savedLog.getId());
        } catch (Exception e) {
            log.warn("Failed to save LLM log: {}", e.getMessage());
        }
    }
}
