package com.haekitchenapp.recipeapp.exception;

public class InvalidValidationCodeException extends RuntimeException  {
    public InvalidValidationCodeException(String message) {
        super(message);
    }

    public InvalidValidationCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
