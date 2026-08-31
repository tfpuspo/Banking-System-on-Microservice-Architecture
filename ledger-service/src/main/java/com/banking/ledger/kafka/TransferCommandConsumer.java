package com.banking.ledger.kafka;

import com.banking.ledger.entity.JournalEntryEntity;
import com.banking.ledger.entity.OutboxEventEntity;
import com.banking.ledger.kafka.dto.DepositRequestedEvent;
import com.banking.ledger.kafka.dto.LedgerResultEvent;
import com.banking.ledger.kafka.dto.TransferRequestedEvent;
import com.banking.ledger.repository.JournalEntryJpaRepository;
import com.banking.ledger.repository.OutboxEventJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Handles every command that can arrive on ledger.commands. Branches on the
 * "eventType" Kafka header (see OutboxPublisher — this is what carries it
 * now, rather than it being lost in transit as it was before this fix).
 *
 * TRANSFER: debit + credit, atomically, only if sufficient balance.
 * DEPOSIT: credit only, no balance check — money enters the system here,
 *          it doesn't move from anywhere, so there's nothing to verify.
 */
@Component
public class TransferCommandConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransferCommandConsumer.class);

    private final JournalEntryJpaRepository journalRepository;
    private final OutboxEventJpaRepository outboxRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TransferCommandConsumer(JournalEntryJpaRepository journalRepository, OutboxEventJpaRepository outboxRepository) {
        this.journalRepository = journalRepository;
        this.outboxRepository = outboxRepository;
    }

    @KafkaListener(topics = KafkaTopics.LEDGER_COMMANDS, groupId = "ledger-service")
    @Transactional
    public void onCommand(String payload, @Header(value = "eventType", required = false) String eventType) {
        if (eventType == null) {
            log.warn("[kafka] received command with no eventType header, ignoring: {}", payload);
            return;
        }

        switch (eventType) {
            case "TransferRequested" -> handleTransfer(payload);
            case "DepositRequested" -> handleDeposit(payload);
            default -> log.warn("[kafka] unknown eventType '{}', ignoring", eventType);
        }
    }

    private void handleTransfer(String payload) {
        TransferRequestedEvent event;
        try {
            event = objectMapper.readValue(payload, TransferRequestedEvent.class);
        } catch (Exception e) {
            log.error("[kafka] could not parse transfer command payload: {}", payload, e);
            return;
        }

        UUID fromAccountId = UUID.fromString(event.fromAccountId());
        UUID toAccountId = UUID.fromString(event.toAccountId());

        if (journalRepository.existsByIdempotencyKeyAndAccountId(event.idempotencyKey(), fromAccountId)) {
            log.info("[kafka] transfer {} already posted, skipping duplicate", event.transactionId());
            return;
        }

        long currentBalance = journalRepository.sumBalanceForAccount(fromAccountId);
        if (currentBalance < event.amountMinorUnits()) {
            publishResult(event.transactionId(), false, "insufficient_funds");
            return;
        }

        UUID transactionId = UUID.fromString(event.transactionId());

        journalRepository.save(new JournalEntryEntity(
            transactionId, fromAccountId, -event.amountMinorUnits(), event.currency(), event.idempotencyKey()
        ));
        journalRepository.save(new JournalEntryEntity(
            transactionId, toAccountId, event.amountMinorUnits(), event.currency(), event.idempotencyKey()
        ));

        publishResult(event.transactionId(), true, null);
        log.info("[ledger] posted transfer {} — {} -> {} ({} {})",
            event.transactionId(), fromAccountId, toAccountId, event.amountMinorUnits(), event.currency());
    }

    private void handleDeposit(String payload) {
        DepositRequestedEvent event;
        try {
            event = objectMapper.readValue(payload, DepositRequestedEvent.class);
        } catch (Exception e) {
            log.error("[kafka] could not parse deposit command payload: {}", payload, e);
            return;
        }

        UUID toAccountId = UUID.fromString(event.toAccountId());

        if (journalRepository.existsByIdempotencyKeyAndAccountId(event.idempotencyKey(), toAccountId)) {
            log.info("[kafka] deposit {} already posted, skipping duplicate", event.transactionId());
            return;
        }

        UUID transactionId = UUID.fromString(event.transactionId());

        journalRepository.save(new JournalEntryEntity(
            transactionId, toAccountId, event.amountMinorUnits(), event.currency(), event.idempotencyKey()
        ));

        publishResult(event.transactionId(), true, null);
        log.info("[ledger] posted deposit {} — {} ({} {})",
            event.transactionId(), toAccountId, event.amountMinorUnits(), event.currency());
    }

    private void publishResult(String transactionId, boolean success, String reason) {
        LedgerResultEvent result = new LedgerResultEvent(transactionId, success, reason);
        try {
            String payload = objectMapper.writeValueAsString(result);
            outboxRepository.save(new OutboxEventEntity(
                KafkaTopics.LEDGER_RESULTS,
                success ? "LedgerEntryPosted" : "LedgerEntryFailed",
                payload
            ));
        } catch (Exception e) {
            log.error("[ledger] failed to serialize result event for {}", transactionId, e);
        }
    }
}
