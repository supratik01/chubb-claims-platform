package com.chubb.claims.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.chubb.claims.domain.model.Role;
import com.chubb.claims.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.JsonNode;

/**
 * The asynchronous half of the system against a real Kafka broker: the outbox relay publishes, the
 * exposure projection consumes, and redelivery or a bad message does no harm.
 * Each test uses its own market (country and currency) so that exposure figures are not shared.
 */
class ClaimEventFlowIntegrationTest extends IntegrationTest {

    private static final Duration PATIENCE = Duration.ofSeconds(20);

    @Autowired
    private KafkaTemplate<String, String> kafka;
    @Autowired
    private ConsumerFactory<?, ?> consumerFactory;
    @Value("${claims.messaging.topic}")
    private String topic;
    @Value("${claims.messaging.dead-letter-topic}")
    private String deadLetterTopic;

    @Test
    void everyStatusChangeIsPublishedToKafkaInOrderAndKeyedByClaim() throws Exception {
        TestUser claimant = signedIn(Role.CLAIMANT);
        TestUser officer = signedIn(Role.CLAIMS_OFFICER);
        String claimId = submitClaim(claimant, policyFor(claimant, "HK", "HKD", "90000.00"), "1200.00");
        postAs(officer, "/api/v1/claims/{id}/assignment", null, claimId);

        List<JsonNode> events = new ArrayList<>();
        try (Consumer<String, String> consumer = consumerFor(topic)) {
            await().atMost(PATIENCE).untilAsserted(() -> {
                for (ConsumerRecord<String, String> message : consumer.poll(Duration.ofMillis(250))) {
                    if (claimId.equals(message.key())) {
                        events.add(json.readTree(message.value()));
                    }
                }
                assertThat(events).hasSize(2);
            });
        }

        JsonNode submitted = events.get(0);
        assertThat(submitted.get("eventType").asString()).isEqualTo("ClaimStatusChanged");
        assertThat(submitted.get("claimId").asString()).isEqualTo(claimId);
        assertThat(submitted.get("previousStatus").isNull()).isTrue();
        assertThat(submitted.get("newStatus").asString()).isEqualTo("SUBMITTED");
        assertThat(submitted.get("countryCode").asString()).isEqualTo("HK");
        assertThat(submitted.get("lineOfBusiness").asString()).isEqualTo("MOTOR");
        assertThat(submitted.get("currency").asString()).isEqualTo("HKD");
        assertThat(submitted.get("previousOutstandingAmount").decimalValue()).isEqualByComparingTo("0");
        assertThat(submitted.get("newOutstandingAmount").decimalValue()).isEqualByComparingTo("1200.00");
        assertThat(submitted.get("changedByUserId").asString()).isEqualTo(claimant.id().toString());

        JsonNode pickedUp = events.get(1);
        assertThat(pickedUp.get("previousStatus").asString()).isEqualTo("SUBMITTED");
        assertThat(pickedUp.get("newStatus").asString()).isEqualTo("UNDER_REVIEW");
        assertThat(pickedUp.get("changedByUserId").asString()).isEqualTo(officer.id().toString());
    }

    @Test
    void outstandingExposureFollowsAClaimThroughItsLifecycle() throws Exception {
        TestUser claimant = signedIn(Role.CLAIMANT);
        TestUser officer = signedIn(Role.CLAIMS_OFFICER);
        TestUser manager = signedIn(Role.CLAIMS_MANAGER);
        UUID policyId = policyFor(claimant, "TH", "THB", "500000.00");

        String firstClaim = submitClaim(claimant, policyId, "40000.00");
        String secondClaim = submitClaim(claimant, policyId, "10000.00");
        awaitExposure(manager, "TH", 2, "50000.00");

        postAs(officer, "/api/v1/claims/{id}/assignment", null, firstClaim);
        postAs(officer, "/api/v1/claims/{id}/decision",
                Map.of("outcome", "APPROVED", "approvedAmount", 35000.00), firstClaim);
        awaitExposure(manager, "TH", 2, "45000.00");

        postAs(officer, "/api/v1/claims/{id}/settlement", null, firstClaim);
        awaitExposure(manager, "TH", 1, "10000.00");

        postAs(officer, "/api/v1/claims/{id}/assignment", null, secondClaim);
        postAs(officer, "/api/v1/claims/{id}/decision",
                Map.of("outcome", "REJECTED", "reason", "Outside the cover"), secondClaim);
        awaitExposure(manager, "TH", 0, "0.00");
    }

    @Test
    void anEventDeliveredTwiceIsCountedOnce() throws Exception {
        TestUser manager = signedIn(Role.CLAIMS_MANAGER);
        String duplicated = submittedEvent("MY", "MYR", "700.00");
        String later = submittedEvent("MY", "MYR", "300.00");

        kafka.send(topic, "claim-a", duplicated).get();
        kafka.send(topic, "claim-a", duplicated).get();
        kafka.send(topic, "claim-a", later).get();

        // Same key, so the three messages are consumed in order: once the last one shows, both copies were seen.
        awaitExposure(manager, "MY", 2, "1000.00");
    }

    @Test
    void anUnreadableEventIsParkedOnTheDeadLetterTopicWithoutBlockingLaterEvents() throws Exception {
        TestUser manager = signedIn(Role.CLAIMS_MANAGER);
        String poison = "this is not a claim event " + UUID.randomUUID();

        kafka.send(topic, "claim-b", poison).get();
        kafka.send(topic, "claim-b", submittedEvent("ID", "IDR", "250.00")).get();

        awaitExposure(manager, "ID", 1, "250.00");
        try (Consumer<String, String> consumer = consumerFor(deadLetterTopic)) {
            List<String> parked = new ArrayList<>();
            await().atMost(PATIENCE).untilAsserted(() -> {
                consumer.poll(Duration.ofMillis(250)).forEach(message -> parked.add(message.value()));
                assertThat(parked).contains(poison);
            });
        }
    }

    private void awaitExposure(TestUser manager, String countryCode, int openClaims, String outstanding) {
        await().atMost(PATIENCE).untilAsserted(() -> {
            JsonNode exposure = body(getAs(manager, "/api/v1/dashboard/exposure"));
            JsonNode line = null;
            for (JsonNode candidate : exposure.get("breakdown")) {
                if (countryCode.equals(candidate.get("countryCode").asString())) {
                    line = candidate;
                }
            }
            assertThat(line).as("exposure line for %s", countryCode).isNotNull();
            assertThat(line.get("openClaims").asInt()).isEqualTo(openClaims);
            assertThat(line.get("outstandingAmount").decimalValue()).isEqualByComparingTo(outstanding);
        });
    }

    private String submittedEvent(String countryCode, String currency, String amount) {
        return json.writeValueAsString(Map.ofEntries(
                Map.entry("eventId", UUID.randomUUID()),
                Map.entry("eventType", "ClaimStatusChanged"),
                Map.entry("occurredAt", "2026-11-15T09:30:00Z"),
                Map.entry("claimId", UUID.randomUUID()),
                Map.entry("claimNumber", "CLM-2026-999999"),
                Map.entry("newStatus", "SUBMITTED"),
                Map.entry("countryCode", countryCode),
                Map.entry("lineOfBusiness", "MOTOR"),
                Map.entry("currency", currency),
                Map.entry("previousOutstandingAmount", 0),
                Map.entry("newOutstandingAmount", new BigDecimal(amount)),
                Map.entry("changedByUserId", UUID.randomUUID())));
    }

    /** A throwaway consumer in its own consumer group, so it reads the topic from the beginning. */
    @SuppressWarnings("unchecked")
    private Consumer<String, String> consumerFor(String topicName) {
        Consumer<String, String> consumer =
                (Consumer<String, String>) consumerFactory.createConsumer("test-" + UUID.randomUUID(), "test");
        consumer.subscribe(List.of(topicName));
        return consumer;
    }
}
