package com.banking.ledger.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Same Outbox pattern as transaction-service's, applied symmetrically here:
 * the journal entries AND the result event are written in one local
 * transaction (see TransferCommandConsumer), so the result is never lost
 * even if Kafka is briefly unavailable when it's time to publish.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String topic;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private boolean published = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected OutboxEventEntity() {}

    public OutboxEventEntity(String topic, String eventType, String payload) {
        this.topic = topic;
        this.eventType = eventType;
        this.payload = payload;
    }

    public UUID getId() { return id; }
    public String getTopic() { return topic; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public boolean isPublished() { return published; }
    public void markPublished() { this.published = true; }
    public Instant getCreatedAt() { return createdAt; }
}
