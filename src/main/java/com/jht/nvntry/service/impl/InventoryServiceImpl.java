package com.jht.nvntry.service.impl;

import com.jht.nvntry.api.event.MovementCreatedEvent;
import com.jht.nvntry.api.exception.InsufficientStockException;
import com.jht.nvntry.domain.common.Reason;
import com.jht.nvntry.domain.inventory.CurrentInventory;
import com.jht.nvntry.domain.inventory.InventoryKey;
import com.jht.nvntry.domain.inventory.InventoryMovement;
import com.jht.nvntry.domain.inventory.dto.ReserveQuantityDTO;
import com.jht.nvntry.domain.reservation.Reservation;
import com.jht.nvntry.domain.reservation.dto.ReservationDTO;
import com.jht.nvntry.domain.reservation.mapper.ReservationMapper;
import com.jht.nvntry.repository.CurrentInventoryRepository;
import com.jht.nvntry.repository.InventoryMovementRepository;
import com.jht.nvntry.repository.ReservationRepository;
import com.jht.nvntry.service.InventoryService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.UUID;

@Service
@Transactional(isolation = Isolation.REPEATABLE_READ) // or SERIALIZABLE if very high contention
public class InventoryServiceImpl implements InventoryService {
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final CurrentInventoryRepository currentInventoryRepo;
    private final InventoryMovementRepository inventoryMovementRepo;
    private final ReservationMapper mapper;
    private final ReservationRepository reservationRepo;

    public InventoryServiceImpl (
            ApplicationEventPublisher eventPublisher,
            Clock clock,
            CurrentInventoryRepository currentInventoryRepo,
            InventoryMovementRepository inventoryMovementRepo,
            ReservationMapper mapper,
            ReservationRepository reservationRepo
    ) {
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.currentInventoryRepo = currentInventoryRepo;
        this.inventoryMovementRepo = inventoryMovementRepo;
        this.mapper = mapper;
        this.reservationRepo = reservationRepo;
    }

    @Override
    public ReservationDTO reserve(
            UUID productId,
            UUID locationId,
            ReserveQuantityDTO dto
    ) {
        if (dto.quantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        InventoryKey key = new InventoryKey();
        key.setProductId(productId);
        key.setLocationId(locationId);
        // 1. Optimistic fast check from MV (for quick failure)
        CurrentInventory stock = currentInventoryRepo.findById(key)
                .orElseThrow(() -> new InsufficientStockException("No stock record for this product/location"));

        if (stock.getQuantity() < dto.quantity()) {
            throw new InsufficientStockException("Insufficient stock: Only " + stock.getQuantity() + " available");
        }

        /* Why This Is Better (and What It Teaches)
         *
         * Fast path + safe path: MV for quick failure, locked aggregate for the final decision.
         * Prevents overselling even under concurrent load.
         * Clear story for interviews: "I used the ledger for auditability, MV for fast reads, and pessimistic locking on the movements for the critical reservation invariant."
         * Transaction isolation + FOR UPDATE gives you real concurrency protection without full serialization.
         *
         * Trade-offs / Blind spots to acknowledge:
         *
         * FOR UPDATE on many movements can cause contention if you have thousands of rows per product/location (rare in your small project).
         * Refresh still inline — next step is moving it to @TransactionalEventListener(AFTER_COMMIT) so it doesn't hold the transaction open.
         * If the lock waits too long, consider adding a timeout (NOWAIT or lock timeout).
         * */
        // 2. Stronger guard: Pessimistic lock via custom query or lock on movements (recommended for prod-like)
        // Example: use a native query with FOR UPDATE on aggregated movements if needed, or rely on transaction isolation
        int lockedAvailable = inventoryMovementRepo.getLockedAvailableQuantity(productId, locationId);
        if (lockedAvailable < dto.quantity()) {
            throw new InsufficientStockException("Insufficient stock (concurrent reservation detected): only "
                    + lockedAvailable + " available");
        }

        // 3. Create reservation first (or after movement — both acceptable; movement is the truth)
        Reservation reservation = Reservation.createActive(productId, locationId, dto.quantity(), clock);
        reservation = reservationRepo.save(reservation);

        // 4. Append immutable movement (the real change)
        InventoryMovement deduction = new InventoryMovement(
                productId,
                locationId,
                reservation.getId(),
                -dto.quantity(), // negative change
                Reason.RESERVATION_CREATED
        );
        inventoryMovementRepo.save(deduction);

        // 5. Refresh the MV so subsequent reads in same or other Tx see the update
        eventPublisher.publishEvent(new MovementCreatedEvent(productId, locationId));

        return mapper.toDTO(reservation);
    }
    // Similar for addStock (receipt), cancelReservation (positive compensating movement), etc.
}
//TODO: Or the concurrent test skeleton?