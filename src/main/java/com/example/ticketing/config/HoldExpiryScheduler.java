package com.example.ticketing.config;

import com.example.ticketing.service.TicketingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Pre-built scheduler — DO NOT MODIFY.
 *
 * Runs every 30 seconds to release expired ticket holds.
 * Calls TicketingService.releaseExpiredHolds() which candidates must implement.
 */
@Component
public class HoldExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(HoldExpiryScheduler.class);
    private final TicketingService ticketingService;

    public HoldExpiryScheduler(TicketingService ticketingService) {
        this.ticketingService = ticketingService;
    }

    @Scheduled(fixedRate = 30_000) // every 30 seconds
    public void expireHolds() {
        try {
            int released = ticketingService.releaseExpiredHolds();
            if (released > 0) {
                log.info("Released {} expired ticket holds", released);
            }
        } catch (Exception e) {
            log.error("Error releasing expired holds", e);
        }
    }
}
