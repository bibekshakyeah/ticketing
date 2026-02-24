package com.example.ticketing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * TODO: Complete this entity class.
 *
 * A TicketHold represents a temporary reservation of tickets. When a user
 * requests tickets, we HOLD them for a short window (default: 2 minutes)
 * while they complete payment. If they don't confirm in time, the hold
 * expires and tickets return to the available pool.
 *
 * Your job:
 * 1. Add appropriate Bean Validation constraints on fields
 * 2. Ensure the status enum is stored correctly in the database
 * 3. Add optimistic locking to prevent concurrent modification
 * 4. Auto-populate createdAt on insert
 * 5. Consider what indexes would improve query performance
 */
@Entity
@Table(
        name = "ticket_holds",
        indexes = {
                @Index(name = "idx_ticket_holds_status_expires", columnList = "status,expiresAt"),
                @Index(name = "idx_ticket_holds_tier_status", columnList = "ticket_tier_id,status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketHold {

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
    private String holdToken;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer quantity;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HoldStatus status;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    private LocalDateTime confirmedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
