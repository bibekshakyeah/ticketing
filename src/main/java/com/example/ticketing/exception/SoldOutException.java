package com.example.ticketing.exception;

/**
 * Pre-built — DO NOT MODIFY.
 * Thrown when requested tickets are not available (availableQuantity < requested).
 */
public class SoldOutException extends RuntimeException {
    public SoldOutException(String message) {
        super(message);
    }
}
