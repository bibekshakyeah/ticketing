package com.example.ticketing.repository;

import com.example.ticketing.entity.HoldStatus;
import com.example.ticketing.entity.TicketHold;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * TODO: Add query methods for hold management.
 *
 * You will need methods to:
 * - Look up a hold by its token
 * - Find expired active holds (for the scheduler cleanup)
 * - Count active holds for a given tier (for availability reporting)
 */
@Repository
public interface TicketHoldRepository extends JpaRepository<TicketHold, Long> {

    Optional<TicketHold> findByHoldToken(String holdToken);

    List<TicketHold> findByStatusAndExpiresAtBefore(HoldStatus status, LocalDateTime expiresBefore);

    long countByTicketTierIdAndStatus(Long ticketTierId, HoldStatus status);

    @Modifying
    @Query("""
            update TicketHold h
            set h.status = :nextStatus
            where h.id = :holdId
              and h.status = :currentStatus
            """)
    int transitionStatus(@Param("holdId") Long holdId,
                         @Param("currentStatus") HoldStatus currentStatus,
                         @Param("nextStatus") HoldStatus nextStatus);

    @Modifying
    @Query("""
            update TicketHold h
            set h.status = :nextStatus,
                h.confirmedAt = :confirmedAt
            where h.id = :holdId
              and h.status = :currentStatus
            """)
    int confirmStatus(@Param("holdId") Long holdId,
                      @Param("currentStatus") HoldStatus currentStatus,
                      @Param("nextStatus") HoldStatus nextStatus,
                      @Param("confirmedAt") LocalDateTime confirmedAt);
}
