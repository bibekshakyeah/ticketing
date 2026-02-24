package com.example.ticketing.repository;

import com.example.ticketing.entity.TicketTier;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TODO: Add custom query methods for atomic inventory operations.
 *
 * You need to implement two critical methods:
 *
 * 1. An atomic decrement that reduces availableQuantity by N,
 *    but ONLY if enough tickets remain. Must return the number of
 *    rows affected (0 means not enough tickets).
 *
 * 2. An atomic increment that releases tickets back to the pool
 *    (used when a hold is cancelled or expires).
 *
 * Think carefully about WHY these must be atomic at the database level,
 * and what annotation(s) Spring Data JPA requires for update queries.
 */
@Repository
public interface TicketTierRepository extends JpaRepository<TicketTier, Long> {

    List<TicketTier> findByEventId(Long eventId);
    Optional<TicketTier> findByIdAndEventId(Long tierId, Long eventId);

    @Modifying
    @Query("""
            update TicketTier t
            set t.availableQuantity = t.availableQuantity - :quantity
            where t.id = :tierId
              and t.event.id = :eventId
              and t.availableQuantity >= :quantity
            """)
    int decrementAvailableQuantity(@Param("tierId") Long tierId,
                                   @Param("eventId") Long eventId,
                                   @Param("quantity") int quantity);

    @Modifying
    @Query("""
            update TicketTier t
            set t.availableQuantity = t.availableQuantity + :quantity
            where t.id = :tierId
            """)
    int incrementAvailableQuantity(@Param("tierId") Long tierId, @Param("quantity") int quantity);
}
