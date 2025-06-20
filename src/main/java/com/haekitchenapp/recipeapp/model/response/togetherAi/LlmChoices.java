package com.haekitchenapp.recipeapp.model.response.togetherAi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.haekitchenapp.recipeapp.model.request.togetherAi.RoleContent;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmChoices {

    private String index;
    private RoleContent message;
    @JsonProperty("finish_reason")
    private String finishReason;
    @JsonProperty("logprobs")
    private String logProblems;

}
