package com.powerassetintelligence.application.port.out;

import com.powerassetintelligence.application.dto.TelemetryAcceptedResponse;
import com.powerassetintelligence.application.dto.TelemetryCreateCommand;

/**
 * Application outbound port for publishing telemetry events.
 *
 * This port abstracts the telemetry event publication mechanism,
 * allowing the application layer to publish events without knowing
 * about Kafka, RabbitMQ, or any other messaging infrastructure.
 *
 * Infrastructure adapters (e.g., TelemetryKafkaProducer) implement this port.
 */
public interface TelemetryEventPublisher {

    /**
     * Publish a telemetry event for asynchronous processing.
     *
     * @param command the telemetry command to publish
     * @return acceptance response with event metadata
     */
    TelemetryAcceptedResponse publish(TelemetryCreateCommand command);
}
