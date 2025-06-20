package com.haekitchenapp.recipeapp.model.request.togetherAi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LLMRequest {

    private LLMRequest(String model, List<RoleContent> messages, List<String> input) {
        this.model = model;
        this.messages = messages;
        this.input = input;
    }


    private String model;
    private List<RoleContent> messages;
    private List<String> input;



    public static LLMRequest getDefaultEmbedRequest(String model) {
        return new LLMRequest(model, null, new ArrayList<>());
    }

    public static LLMRequest getDefaultEmbedRequest(String model, List<String> input) {
        return new LLMRequest(model, null, input);
    }

    public static LLMRequest getDefaultChatRequest(String model) {
        return new LLMRequest(model, new ArrayList<>(List.of(getSystemRecipeRole())), null);
    }

    public static RoleContent getSystemRecipeRole() {
        return new RoleContent("system", "You are a helpful assistant that rewrites cooking instructions to " +
                "be clearer and easier to read.");
    }

}
