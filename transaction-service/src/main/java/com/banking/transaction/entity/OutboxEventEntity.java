package com.banking.transaction.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * The Transactional Outbox pattern: instead of writing a business record AND
 * publishing a Kafka event as two separate operations (which can fail
 * halfway — the "dual write" problem), both happen in ONE local database
 * transaction. A separate poller (OutboxPublisher) reads unpublished rows
 * and relays them to Kafka afterward. If Kafka is briefly unavailable, the
 * event is never lost — it just waits in this table until the poller can
 * send it.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "topic", nullable = false)
    private String topic;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload; // JSON, serialized by the caller

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
