package com.haekitchenapp.recipeapp.service;

import com.haekitchenapp.recipeapp.entity.LlmModelPrice;
import com.haekitchenapp.recipeapp.entity.LlmQueryLog;
import com.haekitchenapp.recipeapp.repository.LlmModelPriceRepository;
import com.haekitchenapp.recipeapp.repository.LlmQueryLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class LlmPricingService {

    private static final Logger logger = LoggerFactory.getLogger(LlmPricingService.class);

    private final LlmModelPriceRepository modelPriceRepository;
    private final LlmQueryLogRepository queryLogRepository;

    @Autowired
    public LlmPricingService(LlmModelPriceRepository modelPriceRepository, LlmQueryLogRepository queryLogRepository) {
        this.modelPriceRepository = modelPriceRepository;
        this.queryLogRepository = queryLogRepository;
    }

    /**
     * Calculates the cost for a given LLM query log and updates the record in the database
     *
     * @param queryLogId The ID of the LlmQueryLog to calculate pricing for
     * @return The updated LlmQueryLog with pricing information, or empty if not found or price not calculated
     */
    @Transactional
    public Optional<LlmQueryLog> calculateAndUpdateQueryPrice(String queryLogId) {
        // Fetch the query log
        Optional<LlmQueryLog> optionalQueryLog = queryLogRepository.findById(queryLogId);

        if (optionalQueryLog.isEmpty()) {
            logger.warn("LLM query log with ID {} not found", queryLogId);
            return Optional.empty();
        }

        LlmQueryLog queryLog = optionalQueryLog.get();

        // Extract model family from the model field (adjust this logic if your model naming is different)
        String modelFamily = queryLog.getModel();

        // Find the matching model price
        Optional<LlmModelPrice> modelPrice = modelPriceRepository.findById(modelFamily);

        if (modelPrice.isEmpty()) {
            logger.warn("No price found for model family: {}", modelFamily);
            return Optional.empty();
        }

        // Calculate costs
        calculateCosts(queryLog, modelPrice.get());

        // Save and return updated query log
        return Optional.of(queryLogRepository.save(queryLog));
    }

    /**
     * Helper method to calculate the costs for a query log based on a model price
     *
     * @param queryLog The query log to update
     * @param modelPrice The model price to use for calculation
     */
    private void calculateCosts(LlmQueryLog queryLog, LlmModelPrice modelPrice) {
        // Convert tokens to millions of tokens
        double promptTokensInMillions = queryLog.getPromptTokens() / 1000000.0;
        double responseTokensInMillions = queryLog.getResponseTokens() / 1000000.0;
        double reasoningTokensInMillions = queryLog.getReasoningTokens() != null ?
                                          queryLog.getReasoningTokens() / 1000000.0 : 0.0;

        // Calculate input cost (prompt + reasoning tokens)
        double inputCost = (promptTokensInMillions + reasoningTokensInMillions) *
                           modelPrice.getInputPerMtokUsd();

        // Calculate output cost
        double outputCost = responseTokensInMillions * modelPrice.getOutputPerMtokUsd();

        // Calculate total cost
        double totalCost = inputCost + outputCost;

        // Update the query log
        queryLog.setInputCost(inputCost);
        queryLog.setOutputCost(outputCost);
        queryLog.setTotalCost(totalCost);
    }

}
