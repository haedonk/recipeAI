package com.haekitchenapp.recipeapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.haekitchenapp.recipeapp.entity.LlmQueryLog;
import com.haekitchenapp.recipeapp.model.request.togetherAi.RoleContent;
import com.haekitchenapp.recipeapp.model.response.togetherAi.LlmChoices;
import com.haekitchenapp.recipeapp.model.response.togetherAi.LlmData;
import com.haekitchenapp.recipeapp.model.response.togetherAi.LlmResponse;
import com.haekitchenapp.recipeapp.model.response.togetherAi.LlmUsage;
import com.haekitchenapp.recipeapp.repository.LlmQueryLogRepository;
import com.openai.models.chat.completions.ChatCompletion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmLoggingServiceTest {

    @Mock
    private LlmQueryLogRepository llmQueryLogRepository;

    @Mock
    private LlmPricingService llmPricingService;

    @InjectMocks
    private LlmLoggingService llmLoggingService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(llmQueryLogRepository.save(any(LlmQueryLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(llmPricingService.calculateAndUpdateQueryPrice(any()))
                .thenReturn(Optional.empty());
    }

    @Test
    void saveQueryLogPersistsUsageAndTriggersPricing() throws Exception {
        ChatCompletion completion = createChatCompletion(new ChatCompletionStub(
                "chatcmpl-123", "response text", new ChatCompletionUsageStub(100L, 60L, 40L, 5L)));

        llmLoggingService.saveQueryLog("gpt-test", "system prompt", "user prompt", completion);

        ArgumentCaptor<LlmQueryLog> logCaptor = ArgumentCaptor.forClass(LlmQueryLog.class);
        verify(llmQueryLogRepository).save(logCaptor.capture());
        LlmQueryLog savedLog = logCaptor.getValue();

        assertThat(savedLog.getId()).isEqualTo("chatcmpl-123");
        assertThat(savedLog.getModel()).isEqualTo("gpt-test");
        assertThat(savedLog.getUserPrompt()).isEqualTo("user prompt");
        assertThat(savedLog.getSystemPrompt()).isEqualTo("system prompt");
        assertThat(savedLog.getResponse()).isEqualTo("response text");
        assertThat(savedLog.getTotalTokens()).isEqualTo(100);
        assertThat(savedLog.getPromptTokens()).isEqualTo(60);
        assertThat(savedLog.getResponseTokens()).isEqualTo(40);
        assertThat(savedLog.getReasoningTokens()).isEqualTo(5);

        verify(llmPricingService).calculateAndUpdateQueryPrice(eq("chatcmpl-123"));
    }

    @Test
    void saveQueryLogHandlesMissingUsage() throws Exception {
        ChatCompletion completion = createChatCompletion(new ChatCompletionStub("chatcmpl-no-usage", "fallback", null));

        llmLoggingService.saveQueryLog("gpt-test", "system prompt", "user prompt", completion);

        ArgumentCaptor<LlmQueryLog> logCaptor = ArgumentCaptor.forClass(LlmQueryLog.class);
        verify(llmQueryLogRepository).save(logCaptor.capture());
        LlmQueryLog savedLog = logCaptor.getValue();

        assertThat(savedLog.getTotalTokens()).isZero();
        assertThat(savedLog.getPromptTokens()).isZero();
        assertThat(savedLog.getResponseTokens()).isZero();
        assertThat(savedLog.getReasoningTokens()).isZero();

        verify(llmPricingService).calculateAndUpdateQueryPrice("chatcmpl-no-usage");
    }

    @Test
    void saveTogetherAiQueryLogPersistsChatResponseAndRecipeId() {
        LlmResponse response = buildChatTogetherAiResponse("together-1", "assistant reply", 120L, 70L, 50L);

        llmLoggingService.saveTogetherAiQueryLog("together-model", "prompt text", response, 42L);

        ArgumentCaptor<LlmQueryLog> logCaptor = ArgumentCaptor.forClass(LlmQueryLog.class);
        verify(llmQueryLogRepository).save(logCaptor.capture());
        LlmQueryLog savedLog = logCaptor.getValue();

        assertThat(savedLog.getId()).isEqualTo("together-1");
        assertThat(savedLog.getUserPrompt()).isEqualTo("prompt text");
        assertThat(savedLog.getResponse()).isEqualTo("assistant reply");
        assertThat(savedLog.getRecipeId()).isEqualTo(42L);
        assertThat(savedLog.getPromptTokens()).isEqualTo(70);
        assertThat(savedLog.getResponseTokens()).isEqualTo(50);
        assertThat(savedLog.getTotalTokens()).isEqualTo(120);

        verify(llmPricingService).calculateAndUpdateQueryPrice("together-1");
    }

    @Test
    void saveTogetherAiQueryLogHandlesEmbeddingAndSwallowsRepositoryErrors() {
        LlmResponse response = buildEmbeddingTogetherAiResponse("embed-1", new Double[]{1.0, 2.0, 3.0}, 30L, 20L, 10L);

        doThrow(new RuntimeException("db down"))
                .when(llmQueryLogRepository)
                .save(any(LlmQueryLog.class));

        assertDoesNotThrow(() -> llmLoggingService.saveTogetherAiQueryLog("embed-model", "embedding prompt", response, 21L));

        verify(llmPricingService, never()).calculateAndUpdateQueryPrice(any());
    }

    @Test
    void saveTogetherAiQueryLogPersistsEmbeddingResponse() {
        Double[] embedding = new Double[]{0.1, 0.2, 0.3};
        LlmResponse response = buildEmbeddingTogetherAiResponse("embed-2", embedding, 45L, 30L, 15L);

        llmLoggingService.saveTogetherAiQueryLog("embed-model", "embedding prompt", response, 100L);

        ArgumentCaptor<LlmQueryLog> logCaptor = ArgumentCaptor.forClass(LlmQueryLog.class);
        verify(llmQueryLogRepository).save(logCaptor.capture());
        LlmQueryLog savedLog = logCaptor.getValue();

        assertThat(savedLog.getId()).isEqualTo("embed-2");
        assertThat(savedLog.getUserPrompt()).isEqualTo("embedding prompt");
        assertThat(savedLog.getRecipeId()).isEqualTo(100L);
        assertThat(savedLog.getResponse()).isEqualTo(Arrays.toString(embedding));
        assertThat(savedLog.getTotalTokens()).isEqualTo(45);
        assertThat(savedLog.getPromptTokens()).isEqualTo(30);
        assertThat(savedLog.getResponseTokens()).isEqualTo(15);

        verify(llmPricingService).calculateAndUpdateQueryPrice("embed-2");
    }

    private ChatCompletion createChatCompletion(ChatCompletionStub stub) throws JsonProcessingException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("id", stub.id());

        ArrayNode choices = root.putArray("choices");
        ObjectNode choiceNode = choices.addObject();
        ObjectNode messageNode = choiceNode.putObject("message");
        messageNode.put("role", "assistant");
        messageNode.put("content", stub.responseText());

        if (stub.usage() != null) {
            ObjectNode usageNode = root.putObject("usage");
            usageNode.put("total_tokens", stub.usage().totalTokens());
            usageNode.put("prompt_tokens", stub.usage().promptTokens());
            usageNode.put("completion_tokens", stub.usage().completionTokens());
            if (stub.usage().reasoningTokens() != null) {
                usageNode.putObject("completion_tokens_details")
                        .put("reasoning_tokens", stub.usage().reasoningTokens());
            }
        }

        return objectMapper.readValue(objectMapper.writeValueAsString(root), ChatCompletion.class);
    }

    private LlmResponse buildChatTogetherAiResponse(String id, String content, Long totalTokens, Long promptTokens, Long completionTokens) {
        LlmResponse response = new LlmResponse();
        response.setId(id);
        response.setModel("together-model");

        LlmChoices choice = new LlmChoices();
        choice.setMessage(new RoleContent("assistant", content));
        response.setChoices(Collections.singletonList(choice));

        LlmUsage usage = new LlmUsage();
        usage.setTotalTokens(totalTokens);
        usage.setPromptTokens(promptTokens);
        usage.setCompletionTokens(completionTokens);
        response.setUsage(usage);
        return response;
    }

    private LlmResponse buildEmbeddingTogetherAiResponse(String id, Double[] embedding, Long totalTokens, Long promptTokens, Long completionTokens) {
        LlmResponse response = new LlmResponse();
        response.setId(id);
        response.setModel("embed-model");

        LlmData data = new LlmData();
        data.setEmbedding(embedding);
        response.setData(Collections.singletonList(data));

        LlmUsage usage = new LlmUsage();
        usage.setTotalTokens(totalTokens);
        usage.setPromptTokens(promptTokens);
        usage.setCompletionTokens(completionTokens);
        response.setUsage(usage);
        return response;
    }

    private record ChatCompletionStub(String id, String responseText, ChatCompletionUsageStub usage) {
    }

    private record ChatCompletionUsageStub(Long totalTokens, Long promptTokens, Long completionTokens, Long reasoningTokens) {
    }
}
