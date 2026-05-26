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

class TelemetryKafkaFlowIT extends BaseIntegrationTest {

    @Autowired
    private TelemetryKafkaProducer producer;

    @Test
    void kafkaProducerShouldSendTelemetryMessage() {
        TelemetryCreateRequest request = new TelemetryCreateRequest(
                UUID.randomUUID(),
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
    }
}
