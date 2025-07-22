package com.portfolio2025.first.exception;

public class NonRetryableMatchException extends RuntimeException {
    public NonRetryableMatchException(String message) {
        super(message);
    }
}
