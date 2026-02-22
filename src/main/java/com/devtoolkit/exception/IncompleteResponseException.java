package com.devtoolkit.exception;

public class IncompleteResponseException extends RuntimeException {
    public IncompleteResponseException(String message) {
        super(message);
    }
}
