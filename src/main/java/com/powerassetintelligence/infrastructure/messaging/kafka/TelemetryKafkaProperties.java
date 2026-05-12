package com.powerassetintelligence.infrastructure.messaging.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.telemetry")
public class TelemetryKafkaProperties {

    /** Topic with raw telemetry accepted from REST/API gateways. */
    private String rawTopic = "telemetry.raw.v1";

    /** Topic for messages that cannot be processed after retries. */
    private String deadLetterTopic = "telemetry.raw.v1.dlt";

    /** Consumer group that persists validated telemetry to PostgreSQL. */
    private String consumerGroup = "power-asset-telemetry-ingestion";

    /** Timeout for REST-to-Kafka broker acknowledgement. */
    private long sendTimeoutMs = 5_000;

    /** Retry attempts before publishing to DLT. */
    private long retryAttempts = 3;

    /** Delay between retry attempts in milliseconds. */
    private long retryBackoffMs = 1_000;

    public String getRawTopic() {
        return rawTopic;
    }

    public void setRawTopic(String rawTopic) {
        this.rawTopic = rawTopic;
    }

    public String getDeadLetterTopic() {
        return deadLetterTopic;
    }

    public void setDeadLetterTopic(String deadLetterTopic) {
        this.deadLetterTopic = deadLetterTopic;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public long getSendTimeoutMs() {
        return sendTimeoutMs;
    }

    public void setSendTimeoutMs(long sendTimeoutMs) {
        this.sendTimeoutMs = sendTimeoutMs;
    }

    public long getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(long retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    public long getRetryBackoffMs() {
        return retryBackoffMs;
    }

    public void setRetryBackoffMs(long retryBackoffMs) {
        this.retryBackoffMs = retryBackoffMs;
    }
}
