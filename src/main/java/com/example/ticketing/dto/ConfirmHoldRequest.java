package com.example.ticketing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class ConfirmHoldRequest {

    @NotBlank(message = "customerName is required")
    @Size(max = 120, message = "customerName must be at most 120 characters")
    private String customerName;

    @NotBlank(message = "customerEmail is required")
    @Email(message = "customerEmail must be a valid email address")
    @Size(max = 255, message = "customerEmail must be at most 255 characters")
    private String customerEmail;
}
