package com.haekitchenapp.recipeapp.exception;

public class HttpError5XXException extends RuntimeException  {
    public HttpError5XXException(String message) {
        super(message);
    }

    public HttpError5XXException(String message, Throwable cause) {
        super(message, cause);
    }
}
