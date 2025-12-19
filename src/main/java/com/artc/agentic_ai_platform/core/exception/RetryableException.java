package com.artc.agentic_ai_platform.core.exception;


public class RetryableException extends RuntimeException {
    public RetryableException(String message) {
        super(message);
    }
}
