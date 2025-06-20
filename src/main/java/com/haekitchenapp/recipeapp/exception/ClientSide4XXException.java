package com.haekitchenapp.recipeapp.exception;

public class ClientSide4XXException extends RuntimeException  {
    public ClientSide4XXException(String message) {
        super(message);
    }

    public ClientSide4XXException(String message, Throwable cause) {
        super(message, cause);
    }
}
