package com.example.ticketing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Pre-built DTO — DO NOT MODIFY.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private String orderReference;
    private Long eventId;
    private String eventName;
    private String tierName;
    private String customerName;
    private String customerEmail;
    private Integer quantity;
    private String totalPrice;
    private LocalDateTime createdAt;
}
