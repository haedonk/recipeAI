package com.haekitchenapp.recipeapp.model.response.togetherAi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmData {
    String index;
    String object;
    Double[] embedding;
}
