package com.haekitchenapp.recipeapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "llm_query_logs")
@Data
@NoArgsConstructor
public class LlmQueryLog {

    public LlmQueryLog(String id, String model, String userPrompt, String systemPrompt, String response, int totalTokens, int responseTokens, int promptTokens, Integer reasoningTokens, Double inputCost, Double outputCost, Double totalCost) {
        this.id = id;
        this.model = model;
        this.userPrompt = userPrompt;
        this.systemPrompt = systemPrompt;
        this.response = response;
        this.totalTokens = totalTokens;
        this.responseTokens = responseTokens;
        this.promptTokens = promptTokens;
        this.reasoningTokens = reasoningTokens;
        this.inputCost = inputCost;
        this.outputCost = outputCost;
        this.totalCost = totalCost;
    }


    public LlmQueryLog(String id, String model, String userPrompt, String systemPrompt, String llmResponse, int i, int i1, int i2, int i3, double v, double v1, double v2, Long recipeId) {
        this(id, model, userPrompt, systemPrompt, llmResponse, i, i1, i2, i3, v, v1, v2);
        this.recipeId = recipeId;
    }

    @Id
    @Column(length = 64) // Together IDs are ~36 chars, 64 is a safe upper bound
    private String id;

    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String userPrompt;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String systemPrompt;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String response;

    @Column(name = "total_tokens", nullable = false)
    private int totalTokens;

    @Column(name = "response_tokens", nullable = false)
    private int responseTokens;

    @Column(name = "prompt_tokens", nullable = false)
    private int promptTokens;

    @Column(name = "reasoning_tokens")
    private Integer reasoningTokens;

    @Column(name = "input_cost")
    private Double inputCost;

    @Column(name = "output_cost")
    private Double outputCost;

    @Column(name = "total_cost")
    private Double totalCost;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;


    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
