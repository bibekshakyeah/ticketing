package com.example.ticketing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Pre-built DTO — DO NOT MODIFY.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventAvailabilityResponse {

    private Long eventId;
    private String eventName;
    private String venue;
    private LocalDateTime eventDate;
    private String saleStatus; // "NOT_ON_SALE", "ON_SALE", "SOLD_OUT"
    private List<TierAvailabilityResponse> tiers;
}
