package com.haekitchenapp.recipeapp.service;

import com.haekitchenapp.recipeapp.entity.LlmModelPrice;
import com.haekitchenapp.recipeapp.entity.LlmQueryLog;
import com.haekitchenapp.recipeapp.repository.LlmModelPriceRepository;
import com.haekitchenapp.recipeapp.repository.LlmQueryLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmPricingServiceTest {

    @Mock
    private LlmModelPriceRepository modelPriceRepository;

    @Mock
    private LlmQueryLogRepository queryLogRepository;

    private LlmPricingService llmPricingService;

    @BeforeEach
    void setUp() {
        llmPricingService = new LlmPricingService(modelPriceRepository, queryLogRepository);
    }

    @Test
    void returnsEmptyWhenQueryLogNotFound() {
        String queryLogId = "missing-id";
        when(queryLogRepository.findById(queryLogId)).thenReturn(Optional.empty());

        Optional<LlmQueryLog> result = llmPricingService.calculateAndUpdateQueryPrice(queryLogId);

        assertThat(result).isEmpty();
        verify(queryLogRepository, never()).save(any());
        verify(modelPriceRepository, never()).findById(any());
    }

    @Test
    void returnsEmptyWhenModelPriceMissing() {
        String queryLogId = "query-id";
        LlmQueryLog queryLog = buildQueryLog();
        when(queryLogRepository.findById(queryLogId)).thenReturn(Optional.of(queryLog));
        when(modelPriceRepository.findById(queryLog.getModel())).thenReturn(Optional.empty());

        Optional<LlmQueryLog> result = llmPricingService.calculateAndUpdateQueryPrice(queryLogId);

        assertThat(result).isEmpty();
        verify(queryLogRepository, never()).save(any());
    }

    @Test
    void calculatesCostsAndSavesLog() {
        String queryLogId = "log-123";
        LlmQueryLog queryLog = buildQueryLog();
        queryLog.setPromptTokens(500);
        queryLog.setResponseTokens(200);
        queryLog.setReasoningTokens(100);

        LlmModelPrice modelPrice = new LlmModelPrice();
        modelPrice.setModelFamily(queryLog.getModel());
        modelPrice.setInputPerMtokUsd(10.0);
        modelPrice.setOutputPerMtokUsd(20.0);

        when(queryLogRepository.findById(queryLogId)).thenReturn(Optional.of(queryLog));
        when(modelPriceRepository.findById(queryLog.getModel())).thenReturn(Optional.of(modelPrice));
        when(queryLogRepository.save(any(LlmQueryLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<LlmQueryLog> result = llmPricingService.calculateAndUpdateQueryPrice(queryLogId);

        assertThat(result).isPresent();
        LlmQueryLog savedLog = result.get();
        assertThat(savedLog.getInputCost()).isCloseTo(0.006, within(1e-9));
        assertThat(savedLog.getOutputCost()).isCloseTo(0.004, within(1e-9));
        assertThat(savedLog.getTotalCost()).isCloseTo(0.01, within(1e-9));

        ArgumentCaptor<LlmQueryLog> captor = ArgumentCaptor.forClass(LlmQueryLog.class);
        verify(queryLogRepository).save(captor.capture());
        LlmQueryLog persisted = captor.getValue();
        assertThat(persisted.getInputCost()).isEqualTo(savedLog.getInputCost());
        assertThat(persisted.getOutputCost()).isEqualTo(savedLog.getOutputCost());
        assertThat(persisted.getTotalCost()).isEqualTo(savedLog.getTotalCost());
    }

    private LlmQueryLog buildQueryLog() {
        LlmQueryLog queryLog = new LlmQueryLog();
        queryLog.setId("query-id");
        queryLog.setModel("gpt-test");
        queryLog.setUserPrompt("user");
        queryLog.setSystemPrompt("system");
        queryLog.setResponse("response");
        queryLog.setTotalTokens(0);
        queryLog.setPromptTokens(0);
        queryLog.setResponseTokens(0);
        queryLog.setReasoningTokens(0);
        queryLog.setRecipeId(1L);
        return queryLog;
    }
}
