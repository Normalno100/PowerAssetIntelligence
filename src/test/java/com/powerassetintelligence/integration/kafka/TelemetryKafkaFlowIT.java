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
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

@EmbeddedKafka(
        partitions = 1,
        topics = {"telemetry.raw.v1", "telemetry.raw.v1.dlt"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@TestPropertySource(properties = {
        "spring.kafka.admin.auto-create=true",
        "spring.kafka.listener.auto-startup=false",
        "app.kafka.telemetry.send-timeout-ms=10000"
})
class TelemetryKafkaFlowIT extends BaseIntegrationTest {

    @Autowired
    private TelemetryKafkaProducer producer;

    @Test
    void kafkaProducerShouldSendTelemetryMessageToEmbeddedTopic() {
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
