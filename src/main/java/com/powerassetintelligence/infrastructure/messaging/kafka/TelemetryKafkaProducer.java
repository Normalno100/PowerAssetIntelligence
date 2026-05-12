package com.powerassetintelligence.infrastructure.messaging.kafka;

import com.powerassetintelligence.application.dto.TelemetryAcceptedResponse;
import com.powerassetintelligence.application.dto.TelemetryCreateRequest;
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

@Component
public class TelemetryKafkaProducer {

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

    public TelemetryAcceptedResponse publish(TelemetryCreateRequest request) {
        UUID eventId = UUID.randomUUID();
        Instant acceptedAt = Instant.now();
        TelemetryMessage message = new TelemetryMessage(
                eventId,
                SCHEMA_VERSION,
                request.assetId(),
                request.timestamp(),
                request.temperatureCelsius(),
                request.loadPercent(),
                request.voltageKv(),
                request.currentAmpere(),
                request.vibrationMmSec(),
                request.overheatingCount(),
                request.sourceSensorId(),
                request.externalTelemetryId(),
                acceptedAt
        );

        String key = request.assetId().toString();
        try {
            SendResult<Object, Object> result = kafkaTemplate.send(properties.getRawTopic(), key, message)
                    .get(properties.getSendTimeoutMs(), TimeUnit.MILLISECONDS);
            RecordMetadata metadata = result.getRecordMetadata();
            log.info(
                    "Published telemetry message. eventId={}, assetId={}, topic={}, partition={}, offset={}",
                    eventId,
                    request.assetId(),
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset()
            );
            return new TelemetryAcceptedResponse(eventId, request.assetId(), "ACCEPTED", acceptedAt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KafkaException("Interrupted while publishing telemetry message", exception);
        } catch (ExecutionException | TimeoutException exception) {
            log.error(
                    "Failed to publish telemetry message. eventId={}, assetId={}, topic={}",
                    eventId,
                    request.assetId(),
                    properties.getRawTopic(),
                    exception
            );
            throw new KafkaException("Failed to publish telemetry message", exception);
        }
    }
}
