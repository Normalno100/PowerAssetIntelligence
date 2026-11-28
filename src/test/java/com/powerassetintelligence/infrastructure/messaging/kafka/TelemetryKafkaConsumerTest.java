package com.powerassetintelligence.infrastructure.messaging.kafka;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.powerassetintelligence.application.dto.TelemetryResponse;
import com.powerassetintelligence.application.service.TelemetryService;
import com.powerassetintelligence.infrastructure.messaging.kafka.message.TelemetryMessage;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.support.Acknowledgment;

class TelemetryKafkaConsumerTest {

    private TelemetryKafkaConsumer consumer;

    @Mock
    private TelemetryService telemetryService;

    @Mock
    private Validator validator;

    @Mock
    private Acknowledgment acknowledgment;

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ASSET_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final Instant TIMESTAMP = Instant.parse("2024-06-01T12:00:00Z");
    private static final Instant RECEIVED_AT = Instant.parse("2024-06-01T12:00:01Z");

    private TelemetryMessage validMessage;
    private ConsumerRecord<String, TelemetryMessage> validRecord;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validMessage = new TelemetryMessage(
                EVENT_ID,
                "1.0",
                ASSET_ID,
                TIMESTAMP,
                BigDecimal.valueOf(75.5),
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(150.0),
                BigDecimal.valueOf(2.5),
                1,
                "sensor-001",
                "ext-tel-001",
                RECEIVED_AT
        );

        validRecord = new ConsumerRecord<>(
                "telemetry.raw.v1",
                0,
                42L,
                ASSET_ID.toString(),
                validMessage
        );

        consumer = new TelemetryKafkaConsumer(telemetryService, validator);
    }

    @SuppressWarnings("unchecked")
    private <T> T mockClass(Class<T> type) {
        return mock(type);
    }

    // ---------------------------------------------------------------------
    // 1. consume — happy path: validates, persists, acknowledges
    // ---------------------------------------------------------------------
    @Test
    void consume_shouldPersistMessageAndAcknowledge() {
        TelemetryResponse response = new TelemetryResponse(
                UUID.randomUUID(),
                ASSET_ID,
                TIMESTAMP,
                BigDecimal.valueOf(75.5),
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(150.0),
                BigDecimal.valueOf(2.5),
                1,
                "sensor-001",
                "ext-tel-001",
                Instant.now()
        );

        when(validator.validate(validMessage)).thenReturn(Set.of());
        when(telemetryService.persist(any())).thenReturn(response);

        consumer.consume(validRecord, acknowledgment);

        verify(validator).validate(validMessage);
        verify(telemetryService).persist(any());
        verify(acknowledgment).acknowledge();
    }

    // ---------------------------------------------------------------------
    // 2. consume — null message throws IllegalArgumentException
    // ---------------------------------------------------------------------
    @Test
    void consume_shouldThrowWhenMessageIsNull() {
        ConsumerRecord<String, TelemetryMessage> nullRecord = new ConsumerRecord<>(
                "telemetry.raw.v1",
                0,
                42L,
                "null-asset",
                null
        );

        assertThrows(IllegalArgumentException.class, () -> consumer.consume(nullRecord, acknowledgment));
        verify(validator, never()).validate(any());
        verify(telemetryService, never()).persist(any());
        verify(acknowledgment, never()).acknowledge();
    }

    // ---------------------------------------------------------------------
    // 3. consume — constraint violation throws ConstraintViolationException
    // ---------------------------------------------------------------------
    @Test
    void consume_shouldThrowConstraintViolationOnInvalidMessage() {
        TelemetryMessage invalidMessage = new TelemetryMessage(
                UUID.randomUUID(),
                "",  // blank schemaVersion — fails @NotBlank
                ASSET_ID,
                TIMESTAMP,
                BigDecimal.valueOf(75.5),
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(150.0),
                BigDecimal.valueOf(2.5),
                1,
                "sensor-001",
                null,
                RECEIVED_AT
        );

        ConsumerRecord<String, TelemetryMessage> invalidRecord = new ConsumerRecord<>(
                "telemetry.raw.v1",
                0,
                43L,
                ASSET_ID.toString(),
                invalidMessage
        );

        Set<ConstraintViolation<TelemetryMessage>> violations = Set.of(
                mockConstraintViolation("schemaVersion")
        );
        when(validator.validate(invalidMessage)).thenReturn(violations);

        ConstraintViolationException exception = assertThrows(
                ConstraintViolationException.class,
                () -> consumer.consume(invalidRecord, acknowledgment)
        );

        assertTrue(exception.getMessage().contains("Invalid telemetry message"));
        verify(telemetryService, never()).persist(any());
        verify(acknowledgment, never()).acknowledge();
    }

    // ---------------------------------------------------------------------
    // 4. consume — maps TelemetryMessage to TelemetryCreateCommand correctly
    // ---------------------------------------------------------------------
    @Test
    void consume_shouldMapMessageToCommandCorrectly() {
        TelemetryResponse response = new TelemetryResponse(
                UUID.randomUUID(),
                ASSET_ID,
                TIMESTAMP,
                BigDecimal.valueOf(75.5),
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(150.0),
                BigDecimal.valueOf(2.5),
                1,
                "sensor-001",
                "ext-tel-001",
                Instant.now()
        );

        when(validator.validate(validMessage)).thenReturn(Set.of());
        when(telemetryService.persist(any())).thenReturn(response);

        consumer.consume(validRecord, acknowledgment);

        verify(telemetryService).persist(argThat(command ->
                command.assetId().equals(ASSET_ID)
                        && command.timestamp().equals(TIMESTAMP)
                        && command.temperatureCelsius().equals(BigDecimal.valueOf(75.5))
                        && command.loadPercent().equals(BigDecimal.valueOf(65.0))
                        && command.voltageKv().equals(BigDecimal.valueOf(110.0))
                        && command.currentAmpere().equals(BigDecimal.valueOf(150.0))
                        && command.vibrationMmSec().equals(BigDecimal.valueOf(2.5))
                        && command.overheatingCount().equals(1)
                        && command.sourceSensorId().equals("sensor-001")
                        && command.externalTelemetryId().equals("ext-tel-001")
        ));
    }

    // ---------------------------------------------------------------------
    // 5. consume — uses externalTelemetryId if provided
    // ---------------------------------------------------------------------
    @Test
    void consume_shouldUseExternalTelemetryIdIfProvided() {
        TelemetryMessage messageWithExternalId = new TelemetryMessage(
                UUID.randomUUID(),
                "1.0",
                ASSET_ID,
                TIMESTAMP,
                BigDecimal.valueOf(75.5),
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(150.0),
                BigDecimal.valueOf(2.5),
                0,
                "sensor-002",
                "custom-ext-id",
                RECEIVED_AT
        );

        ConsumerRecord<String, TelemetryMessage> record = new ConsumerRecord<>(
                "telemetry.raw.v1",
                0,
                44L,
                ASSET_ID.toString(),
                messageWithExternalId
        );

        TelemetryResponse response = new TelemetryResponse(
                UUID.randomUUID(),
                ASSET_ID,
                TIMESTAMP,
                BigDecimal.valueOf(75.5),
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(150.0),
                BigDecimal.valueOf(2.5),
                0,
                "sensor-002",
                "custom-ext-id",
                Instant.now()
        );

        when(validator.validate(messageWithExternalId)).thenReturn(Set.of());
        when(telemetryService.persist(any())).thenReturn(response);

        consumer.consume(record, acknowledgment);

        verify(telemetryService).persist(argThat(command ->
                "custom-ext-id".equals(command.externalTelemetryId())
        ));
    }

    // ---------------------------------------------------------------------
    // 6. consume — uses eventId as fallback when externalTelemetryId is null
    // ---------------------------------------------------------------------
    @Test
    void consume_shouldUseEventIdAsFallbackForExternalTelemetryId() {
        TelemetryMessage messageWithoutExternalId = new TelemetryMessage(
                EVENT_ID,
                "1.0",
                ASSET_ID,
                TIMESTAMP,
                BigDecimal.valueOf(75.5),
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(150.0),
                BigDecimal.valueOf(2.5),
                0,
                "sensor-003",
                null,  // No externalTelemetryId
                RECEIVED_AT
        );

        ConsumerRecord<String, TelemetryMessage> record = new ConsumerRecord<>(
                "telemetry.raw.v1",
                0,
                45L,
                ASSET_ID.toString(),
                messageWithoutExternalId
        );

        TelemetryResponse response = new TelemetryResponse(
                UUID.randomUUID(),
                ASSET_ID,
                TIMESTAMP,
                BigDecimal.valueOf(75.5),
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(150.0),
                BigDecimal.valueOf(2.5),
                0,
                "sensor-003",
                null,
                Instant.now()
        );

        when(validator.validate(messageWithoutExternalId)).thenReturn(Set.of());
        when(telemetryService.persist(any())).thenReturn(response);

        consumer.consume(record, acknowledgment);

        verify(telemetryService).persist(argThat(command ->
                EVENT_ID.toString().equals(command.externalTelemetryId())
        ));
    }

    // ---------------------------------------------------------------------
    // 7. consume — service exception propagates (no ack)
    // ---------------------------------------------------------------------
    @Test
    void consume_shouldNotAcknowledgeOnServiceFailure() {
        when(validator.validate(validMessage)).thenReturn(Set.of());
        when(telemetryService.persist(any()))
                .thenThrow(new RuntimeException("Database connection failed"));

        assertThrows(RuntimeException.class, () -> consumer.consume(validRecord, acknowledgment));

        verify(acknowledgment, never()).acknowledge();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private ConstraintViolation<TelemetryMessage> mockConstraintViolation(String property) {
        jakarta.validation.Path path = mock(jakarta.validation.Path.class);
        when(path.toString()).thenReturn(property);
        ConstraintViolation<TelemetryMessage> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be blank");
        return violation;
    }
}
