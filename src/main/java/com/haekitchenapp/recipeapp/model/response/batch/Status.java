package com.haekitchenapp.recipeapp.model.response.batch;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Status {
    private Long total;
    private Long completed;
    private Float percentage;
    private Long failed;
}
