package com.example.ticketing.config;

import com.example.ticketing.entity.Event;
import com.example.ticketing.entity.TicketTier;
import com.example.ticketing.repository.EventRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pre-built — DO NOT MODIFY.
 *
 * Seeds the database with test data:
 *
 * Event 1: "Taylor Swift — Eras Tour" at "National Stadium"
 *   - VIP Floor:    50 tickets @ $350.00
 *   - Gold Circle: 200 tickets @ $200.00
 *   - General:     500 tickets @ $95.00
 *   - Balcony:     250 tickets @ $55.00
 *   Total: 1000 tickets
 *   On sale: already on sale (onsaleTime = yesterday)
 *
 * Event 2: "Champions League Final" at "Wembley Stadium"
 *   - Category 1: 100 tickets @ $500.00
 *   - Category 2: 300 tickets @ $250.00
 *   - Category 3: 600 tickets @ $120.00
 *   Total: 1000 tickets
 *   On sale: NOT yet (onsaleTime = tomorrow) — useful for testing NotOnSaleException
 */
@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner seedData(EventRepository eventRepository) {
        return args -> {
            if (eventRepository.count() > 0) return;

            // Event 1 — already on sale
            Event concert = Event.builder()
                    .name("Taylor Swift — Eras Tour")
                    .venue("National Stadium")
                    .eventDate(LocalDateTime.now().plusMonths(2))
                    .onsaleTime(LocalDateTime.now().minusDays(1)) // already on sale
                    .build();

            concert.getTiers().add(TicketTier.builder()
                    .event(concert).tierName("VIP Floor").price(new BigDecimal("350.00"))
                    .totalQuantity(50).availableQuantity(50).build());
            concert.getTiers().add(TicketTier.builder()
                    .event(concert).tierName("Gold Circle").price(new BigDecimal("200.00"))
                    .totalQuantity(200).availableQuantity(200).build());
            concert.getTiers().add(TicketTier.builder()
                    .event(concert).tierName("General Admission").price(new BigDecimal("95.00"))
                    .totalQuantity(500).availableQuantity(500).build());
            concert.getTiers().add(TicketTier.builder()
                    .event(concert).tierName("Balcony").price(new BigDecimal("55.00"))
                    .totalQuantity(250).availableQuantity(250).build());

            eventRepository.save(concert);

            // Event 2 — not yet on sale
            Event match = Event.builder()
                    .name("Champions League Final")
                    .venue("Wembley Stadium")
                    .eventDate(LocalDateTime.now().plusMonths(3))
                    .onsaleTime(LocalDateTime.now().plusDays(1)) // NOT yet on sale
                    .build();

            match.getTiers().add(TicketTier.builder()
                    .event(match).tierName("Category 1").price(new BigDecimal("500.00"))
                    .totalQuantity(100).availableQuantity(100).build());
            match.getTiers().add(TicketTier.builder()
                    .event(match).tierName("Category 2").price(new BigDecimal("250.00"))
                    .totalQuantity(300).availableQuantity(300).build());
            match.getTiers().add(TicketTier.builder()
                    .event(match).tierName("Category 3").price(new BigDecimal("120.00"))
                    .totalQuantity(600).availableQuantity(600).build());

            eventRepository.save(match);
        };
    }
}
