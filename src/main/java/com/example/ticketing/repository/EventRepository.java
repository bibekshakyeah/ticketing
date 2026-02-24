package com.example.ticketing.repository;

import com.example.ticketing.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Pre-built — DO NOT MODIFY.
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    Event findByIdAndOnsaleTimeAfter(Long id, LocalDateTime onsaleTimeAfter);
}
