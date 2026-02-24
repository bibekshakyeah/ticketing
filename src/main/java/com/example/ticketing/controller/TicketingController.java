package com.example.ticketing.controller;

import com.example.ticketing.dto.ConfirmHoldRequest;
import com.example.ticketing.dto.EventAvailabilityResponse;
import com.example.ticketing.dto.HoldResponse;
import com.example.ticketing.dto.HoldTicketRequest;
import com.example.ticketing.dto.OrderResponse;
import jakarta.validation.Valid;
import com.example.ticketing.service.TicketingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TODO: Implement all REST endpoints.
 *
 * Refer to the API Reference tab in INSTRUCTIONS.html for the exact
 * endpoint paths, HTTP methods, request/response bodies, and status codes.
 *
 * Requirements:
 * - Validate request bodies
 * - Use appropriate HTTP status codes for each operation
 * - Delegate to TicketingService — keep the controller thin
 */
@RestController
@RequestMapping("/api")
public class TicketingController {

    private final TicketingService ticketingService;

    public TicketingController(TicketingService ticketingService) {
        this.ticketingService = ticketingService;
    }

    @PostMapping("/events/{eventId}/hold")
    @ResponseStatus(HttpStatus.CREATED)
    public HoldResponse holdTickets(@PathVariable Long eventId, @Valid @RequestBody HoldTicketRequest request) {
        return ticketingService.holdTickets(eventId, request);
    }

    @PostMapping("/holds/{holdToken}/confirm")
    public OrderResponse confirmHold(@PathVariable String holdToken, @Valid @RequestBody ConfirmHoldRequest request) {
        return ticketingService.confirmHold(holdToken, request);
    }

    @DeleteMapping("/holds/{holdToken}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelHold(@PathVariable String holdToken) {
        ticketingService.cancelHold(holdToken);
    }

    @GetMapping("/events/{eventId}/availability")
    public EventAvailabilityResponse getAvailability(@PathVariable Long eventId) {
        return ticketingService.getEventAvailability(eventId);
    }

    @GetMapping("/events/{eventId}/orders")
    public List<OrderResponse> getOrders(@PathVariable Long eventId) {
        return ticketingService.getEventOrders(eventId);
    }
}
