package com.buy01.product.service;

import com.buy01.product.event.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditEventProducer {

    private static final String TOPIC = "audit-events";

    private final KafkaTemplate<String, AuditEvent> kafkaTemplate;

    public void send(AuditEvent event) {
        log.info("Sending audit event to Kafka topic {}: {}", TOPIC, event);
        kafkaTemplate.send(TOPIC, event.entityId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send audit event: {}", event, ex);
                    } else {
                        log.debug("Audit event successfully sent to topic {}", TOPIC);
                    }
                });
    }
}
