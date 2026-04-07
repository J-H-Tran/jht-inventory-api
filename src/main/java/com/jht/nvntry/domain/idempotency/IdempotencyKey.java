package com.jht.nvntry.domain.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
public class IdempotencyKey {

    @Id
    private String key;

    @Column(name = "payload_hash", nullable = false)
    private String payloadHash;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "response_body")
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public IdempotencyKey() {
    }

    public IdempotencyKey(
            String key,
            String payloadHash,
            UUID resourceId,
            String responseBody,
            Instant createdAt
    ) {
        this.key = key;
        this.payloadHash = payloadHash;
        this.resourceId = resourceId;
        this.responseBody = responseBody;
        this.createdAt = createdAt;
    }
}