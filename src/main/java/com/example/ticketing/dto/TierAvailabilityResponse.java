package com.example.ticketing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pre-built DTO — DO NOT MODIFY.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TierAvailabilityResponse {

    private Long tierId;
    private String tierName;
    private String price;
    private Integer totalQuantity;
    private Integer availableQuantity;
    private String status; // "AVAILABLE", "LOW_STOCK", "SOLD_OUT"
}
