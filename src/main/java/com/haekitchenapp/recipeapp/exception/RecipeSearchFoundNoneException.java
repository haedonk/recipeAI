package com.haekitchenapp.recipeapp.exception;

public class RecipeSearchFoundNoneException extends Throwable {
    public RecipeSearchFoundNoneException(String message) {
        super(message);
    }

    public RecipeSearchFoundNoneException(String message, Throwable cause) {
        super(message, cause);
    }
}
