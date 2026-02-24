package com.example.ticketing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Pre-built entity — DO NOT MODIFY.
 *
 * A ticket tier (e.g., "VIP Floor", "General Admission", "Balcony").
 * Each tier has a fixed total quantity and a mutable available quantity.
 *
 * CRITICAL: The availableQuantity field is the concurrency hotspot.
 * Candidates must write atomic SQL to decrement/increment this safely.
 */
@Entity
@Table(name = "ticket_tiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private String tierName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Total tickets ever available for this tier. Immutable after creation.
     */
    @Column(nullable = false)
    private Integer totalQuantity;

    /**
     * Current available quantity. Decremented on hold, incremented on release/expiry.
     * THIS IS THE HOT FIELD — must be updated atomically.
     */
    @Column(nullable = false)
    private Integer availableQuantity;

    @Version
    private Long version;
}
