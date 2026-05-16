package com.powerassetintelligence.infrastructure.messaging.kafka;

import com.powerassetintelligence.application.service.BusinessValidationException;
import jakarta.validation.ConstraintViolationException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableConfigurationProperties(TelemetryKafkaProperties.class)
public class KafkaTelemetryConfiguration {

    private static final Logger log = LoggerFactory.getLogger(KafkaTelemetryConfiguration.class);

    @Bean
    NewTopic telemetryRawTopic(TelemetryKafkaProperties properties) {
        return TopicBuilder.name(properties.getRawTopic())
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic telemetryDeadLetterTopic(TelemetryKafkaProperties properties) {
        return TopicBuilder.name(properties.getDeadLetterTopic())
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    DefaultErrorHandler telemetryKafkaErrorHandler(
            KafkaOperations<Object, Object> kafkaOperations,
            TelemetryKafkaProperties properties
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (record, exception) -> resolveDeadLetterTopic(record, exception, properties)
        );

        long retryAttempts = Math.max(properties.getRetryAttempts() - 1, 0);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(properties.getRetryBackoffMs(), retryAttempts)
        );
        errorHandler.addNotRetryableExceptions(
                ConstraintViolationException.class,
                IllegalArgumentException.class,
                BusinessValidationException.class
        );
        errorHandler.setRetryListeners((record, exception, deliveryAttempt) -> log.warn(
                "Retrying telemetry message. topic={}, partition={}, offset={}, attempt={}, error={}",
                record.topic(),
                record.partition(),
                record.offset(),
                deliveryAttempt,
                exception.getMessage()
        ));
        return errorHandler;
    }

    private TopicPartition resolveDeadLetterTopic(
            ConsumerRecord<?, ?> record,
            Exception exception,
            TelemetryKafkaProperties properties
    ) {
        log.error(
                "Publishing telemetry message to DLT. sourceTopic={}, partition={}, offset={}, dltTopic={}, error={}",
                record.topic(),
                record.partition(),
                record.offset(),
                properties.getDeadLetterTopic(),
                exception.getMessage(),
                exception
        );
        return new TopicPartition(properties.getDeadLetterTopic(), record.partition());
    }
}
