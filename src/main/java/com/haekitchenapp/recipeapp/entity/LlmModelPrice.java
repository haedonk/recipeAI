package com.haekitchenapp.recipeapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Table(name = "llm_model_prices")
@Data
@NoArgsConstructor
public class LlmModelPrice {

    @Id
    @Column(name = "model_family")
    private String modelFamily;

    @Column(name = "input_per_mtok_usd", nullable = false)
    private Double inputPerMtokUsd;

    @Column(name = "output_per_mtok_usd", nullable = false)
    private Double outputPerMtokUsd;

    @Column(name = "effective_from", columnDefinition = "timestamptz")
    private ZonedDateTime effectiveFrom;

    @PrePersist
    protected void onCreate() {
        if (this.effectiveFrom == null) {
            this.effectiveFrom = ZonedDateTime.now();
        }
    }
}
