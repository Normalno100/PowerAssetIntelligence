package com.powerassetintelligence.integration.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.powerassetintelligence.application.dto.TelemetryAcceptedResponse;
import com.powerassetintelligence.application.dto.TelemetryCreateRequest;
import com.powerassetintelligence.infrastructure.messaging.kafka.TelemetryKafkaProducer;
import com.powerassetintelligence.testsupport.BaseIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

class TelemetryKafkaFlowIT extends BaseIntegrationTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1")
    );

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.admin.auto-create", () -> true);
        registry.add("app.kafka.telemetry.send-timeout-ms", () -> 10_000);
    }

    @Autowired
    private TelemetryKafkaProducer producer;

    @Test
    void kafkaProducerShouldSendTelemetryMessageToContainerTopic() {
        UUID assetId = UUID.randomUUID();
        TelemetryCreateRequest request = new TelemetryCreateRequest(
                assetId,
                Instant.now(),
                BigDecimal.valueOf(75.0),
                BigDecimal.valueOf(65.0),
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(150.0),
                BigDecimal.valueOf(2.0),
                0,
                "sensor-A",
                "ext-" + UUID.randomUUID()
        );

        TelemetryAcceptedResponse result = producer.publish(request);

        assertThat(result).isNotNull();
        assertThat(result.eventId()).isNotNull();
        assertThat(result.assetId()).isEqualTo(assetId);
        assertThat(result.status()).isEqualTo("ACCEPTED");
        assertThat(result.acceptedAt()).isNotNull();
    }
}
