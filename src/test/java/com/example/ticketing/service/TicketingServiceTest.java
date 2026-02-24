package com.example.ticketing.service;

import com.example.ticketing.dto.ConfirmHoldRequest;
import com.example.ticketing.dto.EventAvailabilityResponse;
import com.example.ticketing.dto.HoldResponse;
import com.example.ticketing.dto.HoldTicketRequest;
import com.example.ticketing.dto.OrderResponse;
import com.example.ticketing.dto.TierAvailabilityResponse;
import com.example.ticketing.entity.HoldStatus;
import com.example.ticketing.entity.TicketHold;
import com.example.ticketing.entity.TicketTier;
import com.example.ticketing.exception.HoldExpiredException;
import com.example.ticketing.exception.NotOnSaleException;
import com.example.ticketing.exception.SoldOutException;
import com.example.ticketing.notification.LoggingNotificationService;
import com.example.ticketing.repository.EventRepository;
import com.example.ticketing.repository.TicketHoldRepository;
import com.example.ticketing.repository.TicketOrderRepository;
import com.example.ticketing.repository.TicketTierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TODO: Write integration tests for the TicketingService.
 *
 * Hints:
 * - Use @SpringBootTest with @ActiveProfiles("test") for H2 database
 * - The DataInitializer seeds 2 events (see DataInitializer.java for details)
 * - Event 1 (Taylor Swift) is already on sale; Event 2 (Champions League) is not yet
 * - Hold duration in test profile is 5 seconds
 */
@SpringBootTest
@ActiveProfiles("test")
class TicketingServiceTest {

    @Autowired
    private TicketingService ticketingService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketTierRepository ticketTierRepository;

    @Autowired
    private TicketHoldRepository ticketHoldRepository;

    @Autowired
    private TicketOrderRepository ticketOrderRepository;

    @Autowired
    private LoggingNotificationService notificationService;

    private Long onSaleEventId;
    private Long notOnSaleEventId;
    private Long vipTierId;
    private Long notOnSaleTierId;

    @BeforeEach
    void setUp() {
        ticketOrderRepository.deleteAll();
        ticketHoldRepository.deleteAll();
        notificationService.clear();
        // Reset available quantities to original values
        ticketTierRepository.findAll().forEach(tier -> {
            tier.setAvailableQuantity(tier.getTotalQuantity());
            ticketTierRepository.save(tier);
        });

        LocalDateTime now = LocalDateTime.now();
        onSaleEventId = eventRepository.findAll().stream()
                .filter(event -> !event.getOnsaleTime().isAfter(now))
                .findFirst()
                .orElseThrow()
                .getId();

        notOnSaleEventId = eventRepository.findAll().stream()
                .filter(event -> event.getOnsaleTime().isAfter(now))
                .findFirst()
                .orElseThrow()
                .getId();

        vipTierId = ticketTierRepository.findByEventId(onSaleEventId).stream()
                .filter(tier -> "VIP Floor".equals(tier.getTierName()))
                .findFirst()
                .orElseThrow()
                .getId();

        notOnSaleTierId = ticketTierRepository.findByEventId(notOnSaleEventId).stream()
                .findFirst()
                .orElseThrow()
                .getId();
    }

    @Nested
    @DisplayName("Hold Tickets")
    class HoldTickets {

        @Test
        @DisplayName("should hold tickets and decrement available quantity")
        void shouldHoldTickets() {
            int before = getTier(vipTierId).getAvailableQuantity();

            HoldResponse response = ticketingService.holdTickets(onSaleEventId, HoldTicketRequest.builder()
                    .tierId(vipTierId)
                    .quantity(2)
                    .build());

            assertThat(response.getHoldToken()).isNotBlank();
            assertThat(response.getEventId()).isEqualTo(onSaleEventId);
            assertThat(response.getTierId()).isEqualTo(vipTierId);
            assertThat(response.getQuantity()).isEqualTo(2);
            assertThat(response.getStatus()).isEqualTo(HoldStatus.ACTIVE.name());
            assertThat(response.getTotalPrice()).isEqualTo("700.00");
            assertThat(response.getExpiresAt()).isAfter(LocalDateTime.now());

            assertThat(getTier(vipTierId).getAvailableQuantity()).isEqualTo(before - 2);
            assertThat(ticketHoldRepository.findByHoldToken(response.getHoldToken()))
                    .isPresent()
                    .get()
                    .extracting(TicketHold::getStatus)
                    .isEqualTo(HoldStatus.ACTIVE);
        }

        @Test
        @DisplayName("should throw SoldOutException when no tickets available")
        void shouldThrowSoldOutWhenNoTickets() {
            TicketTier tier = getTier(vipTierId);
            tier.setAvailableQuantity(0);
            ticketTierRepository.save(tier);

            assertThatThrownBy(() -> ticketingService.holdTickets(onSaleEventId, HoldTicketRequest.builder()
                    .tierId(vipTierId)
                    .quantity(1)
                    .build()))
                    .isInstanceOf(SoldOutException.class);
        }

        @Test
        @DisplayName("should throw NotOnSaleException before onsale time")
        void shouldRejectHoldBeforeOnsale() {
            assertThatThrownBy(() -> ticketingService.holdTickets(notOnSaleEventId, HoldTicketRequest.builder()
                    .tierId(notOnSaleTierId)
                    .quantity(1)
                    .build()))
                    .isInstanceOf(NotOnSaleException.class);
        }
    }

    @Nested
    @DisplayName("Confirm Hold")
    class ConfirmHold {

        @Test
        @DisplayName("should confirm hold and create order with correct total price")
        void shouldConfirmHoldAndCreateOrder() {
            HoldResponse hold = ticketingService.holdTickets(onSaleEventId, HoldTicketRequest.builder()
                    .tierId(vipTierId)
                    .quantity(2)
                    .build());

            OrderResponse order = ticketingService.confirmHold(hold.getHoldToken(), ConfirmHoldRequest.builder()
                    .customerName("John Doe")
                    .customerEmail("john@example.com")
                    .build());

            assertThat(order.getOrderReference()).isNotBlank();
            assertThat(order.getEventId()).isEqualTo(onSaleEventId);
            assertThat(order.getTierName()).isEqualTo("VIP Floor");
            assertThat(order.getCustomerName()).isEqualTo("John Doe");
            assertThat(order.getCustomerEmail()).isEqualTo("john@example.com");
            assertThat(order.getQuantity()).isEqualTo(2);
            assertThat(order.getTotalPrice()).isEqualTo("700.00");
            assertThat(order.getCreatedAt()).isNotNull();

            assertThat(ticketOrderRepository.findByOrderReference(order.getOrderReference())).isPresent();
            assertThat(ticketHoldRepository.findByHoldToken(hold.getHoldToken()))
                    .isPresent()
                    .get()
                    .extracting(TicketHold::getStatus)
                    .isEqualTo(HoldStatus.CONFIRMED);
        }

        @Test
        @DisplayName("should reject confirmation of expired hold")
        void shouldRejectExpiredHold() {
            HoldResponse hold = ticketingService.holdTickets(onSaleEventId, HoldTicketRequest.builder()
                    .tierId(vipTierId)
                    .quantity(1)
                    .build());

            TicketHold savedHold = ticketHoldRepository.findByHoldToken(hold.getHoldToken()).orElseThrow();
            savedHold.setExpiresAt(LocalDateTime.now().minusSeconds(1));
            ticketHoldRepository.save(savedHold);

            assertThatThrownBy(() -> ticketingService.confirmHold(hold.getHoldToken(), ConfirmHoldRequest.builder()
                    .customerName("John Doe")
                    .customerEmail("john@example.com")
                    .build()))
                    .isInstanceOf(HoldExpiredException.class);
        }
    }

    @Nested
    @DisplayName("Cancel Hold")
    class CancelHold {

        @Test
        @DisplayName("should cancel hold and release tickets back to pool")
        void shouldCancelAndReleaseTickets() {
            int before = getTier(vipTierId).getAvailableQuantity();
            HoldResponse hold = ticketingService.holdTickets(onSaleEventId, HoldTicketRequest.builder()
                    .tierId(vipTierId)
                    .quantity(3)
                    .build());

            assertThat(getTier(vipTierId).getAvailableQuantity()).isEqualTo(before - 3);

            ticketingService.cancelHold(hold.getHoldToken());

            assertThat(getTier(vipTierId).getAvailableQuantity()).isEqualTo(before);
            assertThat(ticketHoldRepository.findByHoldToken(hold.getHoldToken()))
                    .isPresent()
                    .get()
                    .extracting(TicketHold::getStatus)
                    .isEqualTo(HoldStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("Expired Hold Cleanup")
    class ExpiredHoldCleanup {

        @Test
        @DisplayName("should release expired holds and return tickets to pool")
        void shouldReleaseExpiredHolds() {
            int before = getTier(vipTierId).getAvailableQuantity();
            HoldResponse hold = ticketingService.holdTickets(onSaleEventId, HoldTicketRequest.builder()
                    .tierId(vipTierId)
                    .quantity(4)
                    .build());

            TicketHold savedHold = ticketHoldRepository.findByHoldToken(hold.getHoldToken()).orElseThrow();
            savedHold.setExpiresAt(LocalDateTime.now().minusSeconds(1));
            ticketHoldRepository.save(savedHold);

            int released = ticketingService.releaseExpiredHolds();

            assertThat(released).isEqualTo(1);
            assertThat(getTier(vipTierId).getAvailableQuantity()).isEqualTo(before);
            assertThat(ticketHoldRepository.findByHoldToken(hold.getHoldToken()))
                    .isPresent()
                    .get()
                    .extracting(TicketHold::getStatus)
                    .isEqualTo(HoldStatus.EXPIRED);
        }
    }

    @Nested
    @DisplayName("Availability")
    class Availability {

        @Test
        @DisplayName("should return correct availability after holds")
        void shouldReturnCorrectAvailability() {
            ticketingService.holdTickets(onSaleEventId, HoldTicketRequest.builder()
                    .tierId(vipTierId)
                    .quantity(46)
                    .build());

            EventAvailabilityResponse availability = ticketingService.getEventAvailability(onSaleEventId);

            assertThat(availability.getEventId()).isEqualTo(onSaleEventId);
            assertThat(availability.getSaleStatus()).isEqualTo("ON_SALE");
            assertThat(availability.getTiers()).hasSizeGreaterThanOrEqualTo(1);

            TierAvailabilityResponse vip = availability.getTiers().stream()
                    .filter(tier -> tier.getTierId().equals(vipTierId))
                    .findFirst()
                    .orElseThrow();

            assertThat(vip.getAvailableQuantity()).isEqualTo(4);
            assertThat(vip.getStatus()).isEqualTo("LOW_STOCK");
        }
    }

    @Nested
    @DisplayName("Email Notification")
    class EmailNotification {

        @Test
        @DisplayName("should send confirmation email when hold is confirmed")
        void shouldSendEmailOnConfirm() {
            HoldResponse hold = ticketingService.holdTickets(onSaleEventId, HoldTicketRequest.builder()
                    .tierId(vipTierId)
                    .quantity(2)
                    .build());

            OrderResponse order = ticketingService.confirmHold(hold.getHoldToken(), ConfirmHoldRequest.builder()
                    .customerName("John Doe")
                    .customerEmail("john@example.com")
                    .build());

            List<LoggingNotificationService.SentMessage> messages = notificationService.getSentMessages();
            assertThat(messages).hasSize(1);
            assertThat(messages.get(0).to()).isEqualTo("john@example.com");
            assertThat(messages.get(0).subject()).contains("Order Confirmation");
            assertThat(messages.get(0).body()).contains(order.getOrderReference());
        }

        @Test
        @DisplayName("should not send email when hold confirmation fails (expired)")
        void shouldNotSendEmailOnFailedConfirm() {
            HoldResponse hold = ticketingService.holdTickets(onSaleEventId, HoldTicketRequest.builder()
                    .tierId(vipTierId)
                    .quantity(1)
                    .build());

            TicketHold savedHold = ticketHoldRepository.findByHoldToken(hold.getHoldToken()).orElseThrow();
            savedHold.setExpiresAt(LocalDateTime.now().minusSeconds(1));
            ticketHoldRepository.save(savedHold);

            assertThatThrownBy(() -> ticketingService.confirmHold(hold.getHoldToken(), ConfirmHoldRequest.builder()
                    .customerName("John Doe")
                    .customerEmail("john@example.com")
                    .build()))
                    .isInstanceOf(HoldExpiredException.class);

            assertThat(notificationService.getSentMessages()).isEmpty();
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  ★  REQUIRED: Concurrent Oversell Prevention
    //
    //  Write a multi-threaded integration test that simulates the "hot onsale"
    //  scenario. This test must PROVE that your system never oversells tickets
    //  even when many users attempt to buy simultaneously.
    //
    //  Scenario:
    //    - A tier has a small number of tickets remaining
    //    - Many more users than tickets try to hold at the exact same instant
    //    - Assert: only the correct number succeed, the rest are rejected
    //    - Assert: available quantity is never negative
    //
    //  This is the most important test in the entire exercise.
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("★ Concurrent Oversell Prevention (REQUIRED)")
    class ConcurrentOversellPrevention {

        @Test
        @DisplayName("many concurrent users fight for limited tickets — no overselling")
        void shouldPreventOversellUnderConcurrentLoad() throws InterruptedException {
            TicketTier tier = getTier(vipTierId);
            tier.setAvailableQuantity(5);
            ticketTierRepository.save(tier);

            int totalThreads = 20;
            ExecutorService pool = Executors.newFixedThreadPool(totalThreads);
            CountDownLatch readyLatch = new CountDownLatch(totalThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(totalThreads);
            AtomicInteger successes = new AtomicInteger();
            AtomicInteger soldOut = new AtomicInteger();
            AtomicInteger unexpected = new AtomicInteger();

            for (int i = 0; i < totalThreads; i++) {
                pool.submit(() -> {
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                        ticketingService.holdTickets(onSaleEventId, HoldTicketRequest.builder()
                                .tierId(vipTierId)
                                .quantity(1)
                                .build());
                        successes.incrementAndGet();
                    } catch (SoldOutException ex) {
                        soldOut.incrementAndGet();
                    } catch (Exception ex) {
                        unexpected.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
            startLatch.countDown();
            assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();

            pool.shutdown();
            assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

            assertThat(successes.get()).isEqualTo(5);
            assertThat(soldOut.get()).isEqualTo(15);
            assertThat(unexpected.get()).isZero();
            assertThat(getTier(vipTierId).getAvailableQuantity()).isZero();
        }
    }

    private TicketTier getTier(Long tierId) {
        return ticketTierRepository.findById(tierId).orElseThrow();
    }
}
