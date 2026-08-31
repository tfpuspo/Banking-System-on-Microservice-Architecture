package com.banking.ledger.kafka;

import com.banking.ledger.entity.OutboxEventEntity;
import com.banking.ledger.repository.OutboxEventJpaRepository;
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
