package com.example.ticketing.exception;

/**
 * Pre-built — DO NOT MODIFY.
 * Thrown when a hold is requested before the event's onsaleTime.
 */
public class NotOnSaleException extends RuntimeException {
    public NotOnSaleException(String message) {
        super(message);
    }
}
