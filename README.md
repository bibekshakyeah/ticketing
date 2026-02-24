# Coding Test: High-Concurrency Event Ticketing

**Duration:** 60 minutes
**Role:** Senior Backend Engineer

## Quick Start

```bash
# 1. Open the instructions
open INSTRUCTIONS.html

# 2. Start PostgreSQL (optional — tests use H2)
docker-compose up -d

# 3. Run tests (uses H2, no Docker needed)
./mvnw test

# 4. Run the app (requires PostgreSQL)
./mvnw spring-boot:run
```

## What to Implement

Look for `TODO` comments in these files:

| File | What to Do |
|------|------------|
| `entity/TicketHold.java` | Add validation, @Version, @PrePersist |
| `entity/TicketOrder.java` | Add validation, @Version, @PrePersist |
| `repository/TicketTierRepository.java` | **Atomic decrement/increment queries** |
| `repository/TicketHoldRepository.java` | Hold lookup queries |
| `repository/TicketOrderRepository.java` | Order lookup queries |
| `service/TicketingService.java` | **All business logic** |
| `controller/TicketingController.java` | REST endpoints |
| `dto/HoldTicketRequest.java` | Bean Validation annotations |
| `dto/ConfirmHoldRequest.java` | Bean Validation annotations |
| `service/TicketingServiceTest.java` | **Integration tests** |

## The Core Problem

10,000 users try to buy 50 VIP tickets simultaneously.
Your system must sell **exactly 50** — not 49, not 51.

See `INSTRUCTIONS.html` for full details and API reference.
