package com.banking.transaction.kafka;

import com.banking.transaction.entity.TransactionEntity;
import com.banking.transaction.kafka.dto.LedgerResultEvent;
import com.banking.transaction.repository.TransactionJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * The Saga's reaction step: ledger-service already made the real decision
 * (posted the journal entries, or refused to). This just reflects that
 * decision back onto the transaction record. No compensating action is
 * needed on failure — if ledger-service never posted anything, there's
 * nothing to undo; marking the transaction FAILED *is* the compensation.
 */
@Component
public class LedgerResultConsumer {

    private static final Logger log = LoggerFactory.getLogger(LedgerResultConsumer.class);

    private final TransactionJpaRepository transactionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LedgerResultConsumer(TransactionJpaRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @KafkaListener(topics = KafkaTopics.LEDGER_RESULTS, groupId = "transaction-service")
    @Transactional
    public void onLedgerResult(String payload) {
        LedgerResultEvent event;
        try {
            event = objectMapper.readValue(payload, LedgerResultEvent.class);
        } catch (Exception e) {
            log.error("[kafka] could not parse ledger result payload: {}", payload, e);
            return;
        }

        Optional<TransactionEntity> transactionOpt = transactionRepository.findById(UUID.fromString(event.transactionId()));
        if (transactionOpt.isEmpty()) {
            log.warn("[kafka] ledger result for unknown transaction {}", event.transactionId());
            return;
        }

        TransactionEntity transaction = transactionOpt.get();

        // Idempotency: Kafka delivers at-least-once, so this handler might
        // run more than once for the same event. Only act if still PENDING.
        if (!transaction.getStatus().equals("PENDING")) {
            log.info("[kafka] transaction {} already {} — ignoring duplicate result", transaction.getId(), transaction.getStatus());
            return;
        }

        if (event.success()) {
            transaction.setStatus("COMPLETED");
        } else {
            transaction.setStatus("FAILED");
            transaction.setFailureReason(event.reason());
        }
        transactionRepository.save(transaction);
        log.info("[kafka] transaction {} -> {}", transaction.getId(), transaction.getStatus());
    }
}
