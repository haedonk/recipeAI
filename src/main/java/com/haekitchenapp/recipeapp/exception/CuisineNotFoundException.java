package com.haekitchenapp.recipeapp.exception;

public class CuisineNotFoundException extends RuntimeException {
    public CuisineNotFoundException(String message) {
        super(message);
    }
}
