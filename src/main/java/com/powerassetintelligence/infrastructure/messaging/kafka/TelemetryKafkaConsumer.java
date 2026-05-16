package com.powerassetintelligence.infrastructure.messaging.kafka;

import com.powerassetintelligence.application.dto.TelemetryCreateRequest;
import com.powerassetintelligence.application.dto.TelemetryResponse;
import com.powerassetintelligence.application.service.TelemetryService;
import com.powerassetintelligence.infrastructure.messaging.kafka.message.TelemetryMessage;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Set;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class TelemetryKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(TelemetryKafkaConsumer.class);

    private final TelemetryService telemetryService;
    private final Validator validator;

    public TelemetryKafkaConsumer(TelemetryService telemetryService, Validator validator) {
        this.telemetryService = telemetryService;
        this.validator = validator;
    }

    @KafkaListener(
            topics = "${app.kafka.telemetry.raw-topic:telemetry.raw.v1}",
            groupId = "${app.kafka.telemetry.consumer-group:power-asset-telemetry-ingestion}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, TelemetryMessage> record, Acknowledgment acknowledgment) {
        TelemetryMessage message = record.value();
        log.info(
                "Consuming telemetry message. eventId={}, assetId={}, topic={}, partition={}, offset={}",
                message == null ? null : message.eventId(),
                message == null ? null : message.assetId(),
                record.topic(),
                record.partition(),
                record.offset()
        );

        validate(message);
        TelemetryResponse response = telemetryService.persist(toRequest(message));
        acknowledgment.acknowledge();

        log.info(
                "Persisted telemetry message. eventId={}, telemetryId={}, assetId={}, timestamp={}",
                message.eventId(),
                response.id(),
                response.assetId(),
                response.timestamp()
        );
    }

    private void validate(TelemetryMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("Telemetry message must not be null");
        }
        Set<ConstraintViolation<TelemetryMessage>> violations = validator.validate(message);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException("Invalid telemetry message", violations);
        }
    }

    private TelemetryCreateRequest toRequest(TelemetryMessage message) {
        return new TelemetryCreateRequest(
                message.assetId(),
                message.timestamp(),
                message.temperatureCelsius(),
                message.loadPercent(),
                message.voltageKv(),
                message.currentAmpere(),
                message.vibrationMmSec(),
                message.overheatingCount(),
                message.sourceSensorId(),
                message.externalTelemetryId() == null ? message.eventId().toString() : message.externalTelemetryId()
        );
    }
}
