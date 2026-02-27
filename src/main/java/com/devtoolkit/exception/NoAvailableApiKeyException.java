package com.devtoolkit.exception;

public class NoAvailableApiKeyException extends RuntimeException {
    public NoAvailableApiKeyException(String message) {
        super(message);
    }
}
