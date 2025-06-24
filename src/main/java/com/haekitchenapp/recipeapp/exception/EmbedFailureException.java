package com.haekitchenapp.recipeapp.exception;

public class EmbedFailureException extends RuntimeException  {
    public EmbedFailureException(String message) {
        super(message);
    }

    public EmbedFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
