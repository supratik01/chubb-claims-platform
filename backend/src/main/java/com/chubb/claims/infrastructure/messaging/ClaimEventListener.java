package com.chubb.claims.infrastructure.messaging;

import com.chubb.claims.service.ExposureProjectionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Kafka entry point of the exposure projection. Translates the message and hands it to the service. */
@Component
class ClaimEventListener {

    private final ExposureProjectionService projection;
    private final ObjectMapper objectMapper;

    ClaimEventListener(ExposureProjectionService projection, ObjectMapper objectMapper) {
        this.projection = projection;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${claims.messaging.topic}", groupId = "${claims.messaging.consumer-group}")
    void onClaimEvent(String payload) {
        ClaimEventMessage message = objectMapper.readValue(payload, ClaimEventMessage.class);
        projection.apply(message.toDomain());
    }
}
