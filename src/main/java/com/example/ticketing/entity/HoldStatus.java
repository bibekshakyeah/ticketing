package com.example.ticketing.entity;

/**
 * Pre-built enum — DO NOT MODIFY.
 */
public enum HoldStatus {
    /** Tickets are temporarily held, awaiting payment confirmation. */
    ACTIVE,

    /** Hold was confirmed — tickets purchased successfully. */
    CONFIRMED,

    /** Hold expired before confirmation (tickets released back to pool). */
    EXPIRED,

    /** Hold was explicitly cancelled by the user. */
    CANCELLED
}
