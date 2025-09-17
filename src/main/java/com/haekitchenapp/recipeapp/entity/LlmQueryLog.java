package com.haekitchenapp.recipeapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "llm_query_logs")
@Data
@NoArgsConstructor
public class LlmQueryLog {

    public LlmQueryLog(String id, String model, String prompt, String response, int totalTokens,
                        int promptTokens, int responseTokens, int reasoningTokens, Long recipeId) {
        this.id = id;
        this.model = model;
        this.prompt = prompt;
        this.response = response;
        this.totalTokens = totalTokens;
        this.promptTokens = promptTokens;
        this.responseTokens = responseTokens;
        this.reasoningTokens = reasoningTokens;
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
    private String prompt;

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
