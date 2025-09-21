package com.haekitchenapp.recipeapp.model.request.togetherAi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class LLMRequestSummarizeDto {

    @NotBlank
    private String model;
    @NotEmpty
    private List<RoleContentDto> messages;

    public LLMRequest toLlmRequest() {
        LLMRequest llmRequest = new LLMRequest();
        llmRequest.setModel(this.model);
        llmRequest.setMessages(this.messages.stream()
                .map(RoleContentDto::toRoleContent)
                .toList());
        return llmRequest;
    }
}
