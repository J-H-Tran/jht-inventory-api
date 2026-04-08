package com.jht.nvntry.api.listener;

import com.jht.nvntry.api.event.MovementCreatedEvent;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MaterializedViewRefresher {
    private final EntityManager em;

    public MaterializedViewRefresher(EntityManager em) {
        this.em = em;
    }
    /* Why This Is Superior (Interview Talking Points)
     *
     * Clear separation: Service handles "reserve and deduct from ledger." Refresher handles "update the read projection."
     * Transactional safety: Refresh only happens on successful commit. If reservation fails/rolls back, no unnecessary refresh.
     * Extensible: Any future operation that adds a movement (addStock, cancel, expiry job) can publish the same event → one refresh handler.
     * Testable: You can mock ApplicationEventPublisher easily. The listener can be tested in isolation.
     * Prod-like: This mirrors real patterns used for cache invalidation, search index updates, etc., after DB changes.
     *
     * Trade-offs to own:
     *
     * Slight delay in MV freshness (until after commit) — perfectly fine for most inventory APIs.
     * If you need immediate freshness inside the same transaction (rare), keep a synchronous refresh for critical paths only.
     * */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refreshAfterMovement(MovementCreatedEvent event) {
        try {
            em.createNativeQuery("REFRESH MATERIALIZED VIEW CONCURRENTLY current_inventory")
                    .executeUpdate();
        } catch (Exception e) {
            // Log warning — do NOT rethrow. MV staleness is acceptable short-term.
            // In real prod, you might fall back to a scheduled refresher or monitoring alert.
        }
    }
}