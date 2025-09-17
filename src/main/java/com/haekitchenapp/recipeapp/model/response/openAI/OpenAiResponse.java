package com.haekitchenapp.recipeapp.model.response.openAI;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.haekitchenapp.recipeapp.model.response.togetherAi.LlmChoices;
import com.haekitchenapp.recipeapp.model.response.togetherAi.LlmData;
import com.haekitchenapp.recipeapp.model.response.togetherAi.LlmUsage;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAiResponse {

    private String id;
    private String model;
    private String object;
    private Long created;
    private List<LlmChoices> choices;
    private LlmUsage usage;
    private List<LlmData> data;

}
