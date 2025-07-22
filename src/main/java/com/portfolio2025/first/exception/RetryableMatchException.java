package com.portfolio2025.first.exception;

public class RetryableMatchException extends RuntimeException {
    public RetryableMatchException(String message) {
        super(message);
    }
}
