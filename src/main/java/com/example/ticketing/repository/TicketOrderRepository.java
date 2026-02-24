package com.example.ticketing.repository;

import com.example.ticketing.entity.TicketOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TODO: Add query methods for order management.
 *
 * You will need methods to look up orders by reference and by event.
 */
@Repository
public interface TicketOrderRepository extends JpaRepository<TicketOrder, Long> {

    Optional<TicketOrder> findByOrderReference(String orderReference);

    List<TicketOrder> findByEventIdOrderByCreatedAtDesc(Long eventId);
}
