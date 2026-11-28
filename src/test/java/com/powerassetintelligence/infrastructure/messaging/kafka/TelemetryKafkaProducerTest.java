package com.powerassetintelligence.infrastructure.messaging.kafka;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.powerassetintelligence.application.dto.TelemetryAcceptedResponse;
import com.powerassetintelligence.application.dto.TelemetryCreateCommand;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

class TelemetryKafkaProducerTest {

    private TelemetryKafkaProducer producer;

    @Mock
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Mock
    private TelemetryKafkaProperties properties;

    private static final UUID ASSET_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final Instant TIMESTAMP = Instant.parse("2024-06-01T12:00:00Z");

    private TelemetryCreateCommand command;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        command = new TelemetryCreateCommand(
                ASSET_ID,
                TIMESTAMP,
                BigDecimal.valueOf(75.5),
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(150.0),
                BigDecimal.valueOf(2.5),
                1,
                "sensor-001",
                "ext-tel-001"
        );

        producer = new TelemetryKafkaProducer(kafkaTemplate, properties);
    }

    // ---------------------------------------------------------------------
    // 1. publish — happy path: sends to Kafka and returns accepted response
    // ---------------------------------------------------------------------
    @Test
    void publish_shouldSendToKafkaAndReturnAccepted() throws Exception {
        when(properties.getRawTopic()).thenReturn("telemetry.raw.v1");
        when(properties.getSendTimeoutMs()).thenReturn(5000L);

        RecordMetadata recordMetadata = mock(RecordMetadata.class);
        when(recordMetadata.topic()).thenReturn("telemetry.raw.v1");
        when(recordMetadata.partition()).thenReturn(0);
        when(recordMetadata.offset()).thenReturn(42L);

        SendResult<Object, Object> sendResult = mock(SendResult.class);
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);

        java.util.concurrent.CompletableFuture<SendResult<Object, Object>> future =
                java.util.concurrent.CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq("telemetry.raw.v1"), anyString(), any())).thenReturn(future);

        TelemetryAcceptedResponse response = producer.publish(command);

        assertNotNull(response);
        assertEquals("ACCEPTED", response.status());
        assertEquals(ASSET_ID, response.assetId());
        assertNotNull(response.eventId());
        assertNotNull(response.acceptedAt());
    }

    // ---------------------------------------------------------------------
    // 2. publish — returns correct event metadata
    // ---------------------------------------------------------------------
    @Test
    void publish_shouldReturnCorrectEventMetadata() throws Exception {
        when(properties.getRawTopic()).thenReturn("telemetry.raw.v1");
        when(properties.getSendTimeoutMs()).thenReturn(5000L);

        RecordMetadata recordMetadata = mock(RecordMetadata.class);
        when(recordMetadata.topic()).thenReturn("telemetry.raw.v1");
        when(recordMetadata.partition()).thenReturn(1);
        when(recordMetadata.offset()).thenReturn(100L);

        SendResult<Object, Object> sendResult = mock(SendResult.class);
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);

        java.util.concurrent.CompletableFuture<SendResult<Object, Object>> future =
                java.util.concurrent.CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq("telemetry.raw.v1"), anyString(), any())).thenReturn(future);

        TelemetryAcceptedResponse response = producer.publish(command);

        // eventId is a new UUID (not the same as assetId)
        assertNotNull(response.eventId());
        assertNotEquals(ASSET_ID, response.eventId());
        assertEquals(ASSET_ID, response.assetId());
        assertEquals("ACCEPTED", response.status());
        assertNotNull(response.acceptedAt());
    }

    // ---------------------------------------------------------------------
    // 3. publish — uses assetId as Kafka message key
    // ---------------------------------------------------------------------
    @Test
    void publish_shouldUseAssetIdAsKey() throws Exception {
        when(properties.getRawTopic()).thenReturn("telemetry.raw.v1");
        when(properties.getSendTimeoutMs()).thenReturn(5000L);

        RecordMetadata recordMetadata = mock(RecordMetadata.class);
        when(recordMetadata.topic()).thenReturn("telemetry.raw.v1");
        when(recordMetadata.partition()).thenReturn(0);
        when(recordMetadata.offset()).thenReturn(42L);

        SendResult<Object, Object> sendResult = mock(SendResult.class);
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);

        java.util.concurrent.CompletableFuture<SendResult<Object, Object>> future =
                java.util.concurrent.CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq("telemetry.raw.v1"), anyString(), any())).thenReturn(future);

        producer.publish(command);

        verify(kafkaTemplate).send(
                eq("telemetry.raw.v1"),
                eq(ASSET_ID.toString()),
                any()
        );
    }

    // ---------------------------------------------------------------------
    // 4. publish — throws KafkaException on timeout
    // ---------------------------------------------------------------------
    @Test
    void publish_shouldThrowKafkaExceptionOnTimeout() throws Exception {
        when(properties.getRawTopic()).thenReturn("telemetry.raw.v1");
        when(properties.getSendTimeoutMs()).thenReturn(1000L);

        java.util.concurrent.CompletableFuture<SendResult<Object, Object>> future =
                new java.util.concurrent.CompletableFuture<>();
        future.completeExceptionally(new java.util.concurrent.TimeoutException("Send timed out"));
        when(kafkaTemplate.send(eq("telemetry.raw.v1"), anyString(), any())).thenReturn(future);

        KafkaException exception = assertThrows(
                KafkaException.class,
                () -> producer.publish(command)
        );

        assertTrue(exception.getMessage().contains("Failed to publish telemetry message"));
    }

    // ---------------------------------------------------------------------
    // 5. publish — throws KafkaException on ExecutionException
    // ---------------------------------------------------------------------
    @Test
    void publish_shouldThrowKafkaExceptionOnExecutionError() throws Exception {
        when(properties.getRawTopic()).thenReturn("telemetry.raw.v1");
        when(properties.getSendTimeoutMs()).thenReturn(5000L);

        java.util.concurrent.CompletableFuture<SendResult<Object, Object>> future =
                new java.util.concurrent.CompletableFuture<>();
        future.completeExceptionally(new ExecutionException(
                "Broker not available", new RuntimeException("Broker not available")));
        when(kafkaTemplate.send(eq("telemetry.raw.v1"), anyString(), any())).thenReturn(future);

        KafkaException exception = assertThrows(
                KafkaException.class,
                () -> producer.publish(command)
        );

        assertTrue(exception.getMessage().contains("Failed to publish telemetry message"));
    }

    // ---------------------------------------------------------------------
    // 6. publish — throws KafkaException on interruption (wrapped in ExecutionException)
    // ---------------------------------------------------------------------
    @Test
    void publish_shouldThrowKafkaExceptionOnInterruptedException() throws Exception {
        when(properties.getRawTopic()).thenReturn("telemetry.raw.v1");
        when(properties.getSendTimeoutMs()).thenReturn(5000L);

        java.util.concurrent.CompletableFuture<SendResult<Object, Object>> future =
                new java.util.concurrent.CompletableFuture<>();
        future.completeExceptionally(new InterruptedException("Send interrupted"));
        when(kafkaTemplate.send(eq("telemetry.raw.v1"), anyString(), any())).thenReturn(future);

        KafkaException exception = assertThrows(
                KafkaException.class,
                () -> producer.publish(command)
        );

        // InterruptedException is wrapped in ExecutionException by CompletableFuture,
        // so it goes through the ExecutionException catch block
        assertTrue(exception.getMessage().contains("Failed to publish telemetry message"));
    }
}
