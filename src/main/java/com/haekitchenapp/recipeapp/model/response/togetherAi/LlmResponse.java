package com.haekitchenapp.recipeapp.model.response.togetherAi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LlmResponse {

    private String id;
    private String model;
    private String object;
    private Long created;
    private List<LlmChoices> choices;
    private LlmUsage usage;
    private List<LlmData> data;

}
