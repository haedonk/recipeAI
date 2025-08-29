package com.haekitchenapp.recipeapp.model.request.recipe;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Arrays;
import java.util.stream.Collectors;

@Data
public class EmbedUpdateRequest {
    @NotNull
    private Long id;
    @NotNull
    @Size(min = 1, message = "Embedding must contain at least one element")
    private Double[] embedding;

    @JsonIgnore
    public String getEmbedString(){
        if (embedding == null || embedding.length == 0) {
            return null;
        }
        return Arrays.stream(embedding)
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }
}
