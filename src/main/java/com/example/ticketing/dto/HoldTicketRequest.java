package com.example.ticketing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TODO: Add Bean Validation annotations on the fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoldTicketRequest {

    @NotNull(message = "tierId is required")
    @Positive(message = "tierId must be positive")
    private Long tierId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;
}
