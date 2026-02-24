package com.example.ticketing.service;

import com.example.ticketing.dto.*;
import com.example.ticketing.entity.Event;
import com.example.ticketing.entity.HoldStatus;
import com.example.ticketing.entity.TicketHold;
import com.example.ticketing.entity.TicketOrder;
import com.example.ticketing.entity.TicketTier;
import com.example.ticketing.exception.EventNotFoundException;
import com.example.ticketing.exception.HoldExpiredException;
import com.example.ticketing.exception.NotOnSaleException;
import com.example.ticketing.exception.SoldOutException;
import com.example.ticketing.notification.NotificationService;
import com.example.ticketing.repository.EventRepository;
import com.example.ticketing.repository.TicketHoldRepository;
import com.example.ticketing.repository.TicketOrderRepository;
import com.example.ticketing.repository.TicketTierRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * TODO: Implement ALL methods in this service.
 *
 * This service handles the full ticket lifecycle: hold → confirm → cancel/expire.
 *
 * Key business rules:
 * - Tickets can only be held after the event's onsaleTime
 * - A hold reserves tickets for a limited time (configured in application.yml)
 * - Confirming a hold creates a permanent order
 * - Cancelling or expiring a hold releases tickets back to the pool
 * - The system must NEVER oversell — even under extreme concurrent load
 *
 * Available exceptions (pre-built):
 * - EventNotFoundException, NotOnSaleException, SoldOutException, HoldExpiredException
 *
 * The HoldExpiryScheduler (pre-built) calls releaseExpiredHolds() every 30 seconds.
 */
@Service
public class TicketingService {

    private final EventRepository eventRepository;
    private final TicketTierRepository ticketTierRepository;
    private final TicketHoldRepository ticketHoldRepository;
    private final TicketOrderRepository ticketOrderRepository;
    private final NotificationService notificationService;

    @Value("${ticketing.hold-duration-seconds:120}")
    private int holdDurationSeconds;

    public TicketingService(EventRepository eventRepository,
                            TicketTierRepository ticketTierRepository,
                            TicketHoldRepository ticketHoldRepository,
                            TicketOrderRepository ticketOrderRepository,
                            NotificationService notificationService) {
        this.eventRepository = eventRepository;
        this.ticketTierRepository = ticketTierRepository;
        this.ticketHoldRepository = ticketHoldRepository;
        this.ticketOrderRepository = ticketOrderRepository;
        this.notificationService = notificationService;
    }

    /**
     * TODO: Hold tickets for a given event and tier.
     *
     * Must handle 10,000+ concurrent requests without overselling.
     */
    @Transactional
    public HoldResponse holdTickets(Long eventId, HoldTicketRequest request) {
        Event event = findEventOrThrow(eventId);
        LocalDateTime now = LocalDateTime.now();
        if (event.getOnsaleTime().isAfter(now)) {
            throw new NotOnSaleException("Tickets are not yet on sale");
        }

        TicketTier tier = ticketTierRepository.findByIdAndEventId(request.getTierId(), eventId)
                .orElseThrow(() -> new IllegalArgumentException("Tier does not belong to the event"));

        int updated = ticketTierRepository.decrementAvailableQuantity(tier.getId(), eventId, request.getQuantity());
        if (updated == 0) {
            throw new SoldOutException("Not enough tickets available");
        }

        LocalDateTime expiresAt = now.plusSeconds(holdDurationSeconds);
        TicketHold hold = TicketHold.builder()
                .event(event)
                .ticketTier(tier)
                .holdToken(UUID.randomUUID().toString())
                .quantity(request.getQuantity())
                .status(HoldStatus.ACTIVE)
                .expiresAt(expiresAt)
                .build();
        ticketHoldRepository.save(hold);

        BigDecimal totalPrice = calculateTotalPrice(tier.getPrice(), request.getQuantity());
        return HoldResponse.builder()
                .holdToken(hold.getHoldToken())
                .eventId(event.getId())
                .tierId(tier.getId())
                .tierName(tier.getTierName())
                .quantity(request.getQuantity())
                .totalPrice(moneyToString(totalPrice))
                .expiresAt(expiresAt)
                .status(HoldStatus.ACTIVE.name())
                .build();
    }

    /**
     * TODO: Confirm a held ticket — convert to a permanent order.
     *
     * Only active, non-expired holds can be confirmed.
     * After successfully creating the order, send a confirmation email
     * to the customer using the NotificationService.
     */
    @Transactional
    public OrderResponse confirmHold(String holdToken, ConfirmHoldRequest request) {
        TicketHold hold = ticketHoldRepository.findByHoldToken(holdToken)
                .orElseThrow(() -> new HoldExpiredException("Hold has expired or been cancelled"));

        if (isExpired(hold)) {
            expireHoldIfActive(hold);
            throw new HoldExpiredException("Hold has expired or been cancelled");
        }

        int transitioned = ticketHoldRepository.confirmStatus(
                hold.getId(),
                HoldStatus.ACTIVE,
                HoldStatus.CONFIRMED,
                LocalDateTime.now()
        );
        if (transitioned == 0) {
            throw new HoldExpiredException("Hold has expired or been cancelled");
        }

        BigDecimal totalPrice = calculateTotalPrice(hold.getTicketTier().getPrice(), hold.getQuantity());
        TicketOrder order = TicketOrder.builder()
                .event(hold.getEvent())
                .ticketTier(hold.getTicketTier())
                .orderReference("ORD-" + UUID.randomUUID())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .quantity(hold.getQuantity())
                .totalPrice(totalPrice)
                .build();

        ticketOrderRepository.save(order);
        sendConfirmationEmail(order);
        return toOrderResponse(order);
    }

    /**
     * TODO: Cancel a hold — release tickets back to the available pool.
     */
    @Transactional
    public void cancelHold(String holdToken) {
        TicketHold hold = ticketHoldRepository.findByHoldToken(holdToken)
                .orElseThrow(() -> new HoldExpiredException("Hold has expired or been cancelled"));

        if (isExpired(hold)) {
            expireHoldIfActive(hold);
            throw new HoldExpiredException("Hold has expired or been cancelled");
        }

        int transitioned = ticketHoldRepository.transitionStatus(hold.getId(), HoldStatus.ACTIVE, HoldStatus.CANCELLED);
        if (transitioned == 0) {
            throw new HoldExpiredException("Hold has expired or been cancelled");
        }

        ticketTierRepository.incrementAvailableQuantity(hold.getTicketTier().getId(), hold.getQuantity());
    }

    /**
     * TODO: Release all expired holds.
     *
     * Called by the scheduler every 30 seconds. Must be safe to run
     * concurrently with user-facing operations (confirm, cancel).
     *
     * @return the number of holds released
     */
    @Transactional
    public int releaseExpiredHolds() {
        List<TicketHold> expiredHolds = ticketHoldRepository.findByStatusAndExpiresAtBefore(
                HoldStatus.ACTIVE,
                LocalDateTime.now()
        );

        int released = 0;
        for (TicketHold hold : expiredHolds) {
            int transitioned = ticketHoldRepository.transitionStatus(hold.getId(), HoldStatus.ACTIVE, HoldStatus.EXPIRED);
            if (transitioned > 0) {
                ticketTierRepository.incrementAvailableQuantity(hold.getTicketTier().getId(), hold.getQuantity());
                released++;
            }
        }
        return released;
    }

    /**
     * TODO: Get availability for all tiers of an event.
     *
     * Each tier should show a status: "AVAILABLE", "LOW_STOCK" (< 10% remaining), or "SOLD_OUT".
     * The overall event saleStatus should be "NOT_ON_SALE", "ON_SALE", or "SOLD_OUT".
     */
    @Transactional(readOnly = true)
    public EventAvailabilityResponse getEventAvailability(Long eventId) {
        Event event = findEventOrThrow(eventId);
        List<TicketTier> tiers = ticketTierRepository.findByEventId(eventId);

        List<TierAvailabilityResponse> tierResponses = tiers.stream()
                .map(this::toTierAvailabilityResponse)
                .toList();

        return EventAvailabilityResponse.builder()
                .eventId(event.getId())
                .eventName(event.getName())
                .venue(event.getVenue())
                .eventDate(event.getEventDate())
                .saleStatus(resolveSaleStatus(event, tiers))
                .tiers(tierResponses)
                .build();
    }

    /**
     * TODO: Get all confirmed orders for an event.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getEventOrders(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new EventNotFoundException("Event not found: " + eventId);
        }

        return ticketOrderRepository.findByEventIdOrderByCreatedAtDesc(eventId).stream()
                .map(this::toOrderResponse)
                .toList();
    }

    private Event findEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + eventId));
    }

    private boolean isExpired(TicketHold hold) {
        return hold.getStatus() != HoldStatus.ACTIVE || !hold.getExpiresAt().isAfter(LocalDateTime.now());
    }

    private void expireHoldIfActive(TicketHold hold) {
        int transitioned = ticketHoldRepository.transitionStatus(hold.getId(), HoldStatus.ACTIVE, HoldStatus.EXPIRED);
        if (transitioned > 0) {
            ticketTierRepository.incrementAvailableQuantity(hold.getTicketTier().getId(), hold.getQuantity());
        }
    }

    private BigDecimal calculateTotalPrice(BigDecimal unitPrice, int quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveSaleStatus(Event event, List<TicketTier> tiers) {
        if (event.getOnsaleTime().isAfter(LocalDateTime.now())) {
            return "NOT_ON_SALE";
        }
        boolean allSoldOut = !tiers.isEmpty() && tiers.stream().allMatch(tier -> tier.getAvailableQuantity() <= 0);
        return allSoldOut ? "SOLD_OUT" : "ON_SALE";
    }

    private TierAvailabilityResponse toTierAvailabilityResponse(TicketTier tier) {
        return TierAvailabilityResponse.builder()
                .tierId(tier.getId())
                .tierName(tier.getTierName())
                .price(moneyToString(tier.getPrice()))
                .totalQuantity(tier.getTotalQuantity())
                .availableQuantity(tier.getAvailableQuantity())
                .status(resolveTierStatus(tier))
                .build();
    }

    private String resolveTierStatus(TicketTier tier) {
        int available = tier.getAvailableQuantity();
        int total = tier.getTotalQuantity();

        if (available <= 0) {
            return "SOLD_OUT";
        }
        if (available * 10 < total) {
            return "LOW_STOCK";
        }
        return "AVAILABLE";
    }

    private OrderResponse toOrderResponse(TicketOrder order) {
        return OrderResponse.builder()
                .orderReference(order.getOrderReference())
                .eventId(order.getEvent().getId())
                .eventName(order.getEvent().getName())
                .tierName(order.getTicketTier().getTierName())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .quantity(order.getQuantity())
                .totalPrice(moneyToString(order.getTotalPrice()))
                .createdAt(order.getCreatedAt())
                .build();
    }

    private void sendConfirmationEmail(TicketOrder order) {
        String subject = "Order Confirmation - " + order.getOrderReference();
        String body = String.format(
                """
                Thank you for your purchase.
                                
                Order Reference: %s
                Event: %s
                Tier: %s
                Quantity: %d
                Total: %s
                """,
                order.getOrderReference(),
                order.getEvent().getName(),
                order.getTicketTier().getTierName(),
                order.getQuantity(),
                moneyToString(order.getTotalPrice())
        );
        notificationService.sendEmail(order.getCustomerEmail(), subject, body);
    }

    private String moneyToString(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
