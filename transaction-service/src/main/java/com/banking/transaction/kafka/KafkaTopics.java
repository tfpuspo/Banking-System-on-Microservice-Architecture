package com.banking.transaction.kafka;

public final class KafkaTopics {
    public static final String LEDGER_COMMANDS = "ledger.commands"; // transaction-service -> ledger-service
    public static final String LEDGER_RESULTS = "ledger.results";   // ledger-service -> transaction-service

    private KafkaTopics() {}
}
