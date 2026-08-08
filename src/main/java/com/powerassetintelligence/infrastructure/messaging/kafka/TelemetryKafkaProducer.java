package com.powerassetintelligence.infrastructure.messaging.kafka;

import com.powerassetintelligence.application.dto.TelemetryAcceptedResponse;
import com.powerassetintelligence.application.dto.TelemetryCreateCommand;
import com.powerassetintelligence.application.port.out.TelemetryEventPublisher;
import com.powerassetintelligence.infrastructure.messaging.kafka.message.TelemetryMessage;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/**
 * Infrastructure adapter that implements {@link TelemetryEventPublisher}.
 *
 * This class handles the technical details of publishing telemetry events
 * to Kafka, including serialization, error handling, and metadata logging.
 * The application layer remains unaware of Kafka — it depends only on
 * the {@link TelemetryEventPublisher} port interface.
 */
@Component
public class TelemetryKafkaProducer implements TelemetryEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TelemetryKafkaProducer.class);
    private static final String SCHEMA_VERSION = "1.0";

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final TelemetryKafkaProperties properties;

    public TelemetryKafkaProducer(
            KafkaTemplate<Object, Object> kafkaTemplate,
            TelemetryKafkaProperties properties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    /**
     * Publishes a telemetry event to Kafka.
     *
     * @param command the application command containing telemetry data
     * @return acceptance response with event metadata
     * @throws KafkaException if publishing fails (timeout, execution, or interruption)
     */
    @Override
    public TelemetryAcceptedResponse publish(TelemetryCreateCommand command) {
        UUID eventId = UUID.randomUUID();
        Instant acceptedAt = Instant.now();
        TelemetryMessage message = new TelemetryMessage(
                eventId,
                SCHEMA_VERSION,
                command.assetId(),
                command.timestamp(),
                command.temperatureCelsius(),
                command.loadPercent(),
                command.voltageKv(),
                command.currentAmpere(),
                command.vibrationMmSec(),
                command.overheatingCount(),
                command.sourceSensorId(),
                command.externalTelemetryId(),
                acceptedAt
        );

        String key = command.assetId().toString();
        try {
            SendResult<Object, Object> result = kafkaTemplate.send(properties.getRawTopic(), key, message)
                    .get(properties.getSendTimeoutMs(), TimeUnit.MILLISECONDS);
            RecordMetadata metadata = result.getRecordMetadata();
            log.info(
                    "Published telemetry message. eventId={}, assetId={}, topic={}, partition={}, offset={}",
                    eventId,
                    command.assetId(),
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset()
            );
            return new TelemetryAcceptedResponse(eventId, command.assetId(), "ACCEPTED", acceptedAt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KafkaException("Interrupted while publishing telemetry message", exception);
        } catch (ExecutionException | TimeoutException exception) {
            log.error(
                    "Failed to publish telemetry message. eventId={}, assetId={}, topic={}",
                    eventId,
                    command.assetId(),
                    properties.getRawTopic(),
                    exception
            );
            throw new KafkaException("Failed to publish telemetry message", exception);
        }
    }
}
