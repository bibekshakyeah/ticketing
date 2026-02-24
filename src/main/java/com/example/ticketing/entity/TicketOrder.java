package com.example.ticketing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TODO: Complete this entity class.
 *
 * A TicketOrder is created when a hold is confirmed (payment succeeds).
 * This is the permanent record of a successful ticket purchase.
 *
 * Your job:
 * 1. Add appropriate Bean Validation constraints on fields
 * 2. Add optimistic locking
 * 3. Auto-populate createdAt on insert
 */
@Entity
@Table(
        name = "ticket_orders",
        indexes = {
                @Index(name = "idx_ticket_orders_event_created", columnList = "event_id,createdAt")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @NotNull
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_tier_id", nullable = false)
    @NotNull
    private TicketTier ticketTier;

    @NotBlank
    @Size(max = 64)
    @Column(nullable = false, unique = true)
    private String orderReference;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false)
    private String customerName;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(nullable = false)
    private String customerEmail;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer quantity;

    @NotNull
    @DecimalMin(value = "0.01")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    private LocalDateTime createdAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
