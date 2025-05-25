package com.haekitchenapp.recipeapp.exception;

public class RecipeNotFoundException extends Throwable {
    public RecipeNotFoundException(String message) {
        super(message);
    }

    public RecipeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
