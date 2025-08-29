package com.haekitchenapp.recipeapp.model.request.togetherAi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class LLMRequestEmbedDto {

    @NotBlank
    private String model;
    @NotEmpty
    private List<String> input;

    public LLMRequest toLlmRequest() {
        LLMRequest llmRequest = new LLMRequest();
        llmRequest.setModel(this.model);
        llmRequest.setInput(this.input);
        return llmRequest;
    }

}
