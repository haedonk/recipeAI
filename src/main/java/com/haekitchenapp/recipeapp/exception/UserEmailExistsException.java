package com.haekitchenapp.recipeapp.exception;

public class UserEmailExistsException extends RuntimeException  {
    public UserEmailExistsException(String message) {
        super(message);
    }

    public UserEmailExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
