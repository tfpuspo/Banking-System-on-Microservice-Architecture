package com.banking.transaction.kafka;

import com.banking.transaction.entity.OutboxEventEntity;
import com.banking.transaction.repository.OutboxEventJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Relays outbox rows to Kafka.
 *
 * Fixed: previously this sent only event.getPayload() as the Kafka message
 * value, silently dropping event.getEventType() — meaning a consumer had
 * no way to tell what KIND of event it received, only its raw JSON shape.
 * That worked by accident while only one event type (TransferRequested)
 * existed. It now attaches eventType as a Kafka message HEADER, so
 * consumers can branch on it before attempting to deserialize — required
 * for deposit/withdrawal to coexist with transfers on the same topic.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventJpaRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxEventJpaRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:500}")
    @Transactional
    public void publishPending() {
        List<OutboxEventEntity> pending = outboxRepository.findTop50ByPublishedFalseOrderByCreatedAtAsc();
        for (OutboxEventEntity event : pending) {
            try {
                Message<String> message = MessageBuilder
                    .withPayload(event.getPayload())
                    .setHeader(KafkaHeaders.TOPIC, event.getTopic())
                    .setHeader(KafkaHeaders.KEY, event.getId().toString())
                    .setHeader("eventType", event.getEventType())
                    .build();

                kafkaTemplate.send(message).get();
                event.markPublished();
                outboxRepository.save(event);
                log.info("[outbox] published {} to {}", event.getEventType(), event.getTopic());
            } catch (Exception e) {
                log.warn("[outbox] failed to publish {}, will retry: {}", event.getId(), e.getMessage());
            }
        }
    }
}
