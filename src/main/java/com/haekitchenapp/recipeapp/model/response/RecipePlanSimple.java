package com.haekitchenapp.recipeapp.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipePlanSimple {
    
    private Long id;
    private Long userId;
    private LocalDate planDate;
    private Short mealTypeId;
    private Long recipeId;
    private String customTitle;
    private String notes;
    private Boolean saved;
}
