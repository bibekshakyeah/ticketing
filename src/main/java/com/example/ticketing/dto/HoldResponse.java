package com.example.ticketing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Pre-built DTO — DO NOT MODIFY.
 *
 * Returned when a hold is successfully created.
 * The holdToken is what the client uses to confirm or cancel.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoldResponse {

    private String holdToken;
    private Long eventId;
    private Long tierId;
    private String tierName;
    private Integer quantity;
    private String totalPrice;
    private LocalDateTime expiresAt;
    private String status;
}
