package com.haekitchenapp.recipeapp.exception;

public class RecipeSearchFoundNoneException extends RuntimeException  {
    public RecipeSearchFoundNoneException(String message) {
        super(message);
    }

    public RecipeSearchFoundNoneException(String message, Throwable cause) {
        super(message, cause);
    }
}
