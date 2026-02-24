package com.example.ticketing.exception;

/**
 * Pre-built — DO NOT MODIFY.
 * Thrown when a client tries to confirm a hold that has already expired or been cancelled.
 */
public class HoldExpiredException extends RuntimeException {
    public HoldExpiredException(String message) {
        super(message);
    }
}
