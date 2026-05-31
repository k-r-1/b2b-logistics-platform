package com.boxoffice.ainotificationservice.notification.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.boxoffice.ainotificationservice.notification.entity.message.NotificationStatus;
import com.boxoffice.ainotificationservice.notification.entity.message.SlackMessage;
import com.boxoffice.ainotificationservice.notification.repository.ProcessedEventRepository;
import com.boxoffice.ainotificationservice.notification.repository.SlackMessageRepository;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notif;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;"
                + "INIT=CREATE SCHEMA IF NOT EXISTS notification_schema",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.autoconfigure.exclude="
                + "org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
@EmbeddedKafka(partitions = 1, topics = {
        "user.events", "order.events", "delivery.events", "delivery-manager.events", "notification.dlq"})
@DisplayName("Kafka 컨슈머 통합")
class KafkaConsumerIntegrationTest {

    private static final String GROUP = "ai-notification-service";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private SlackMessageRepository slackMessageRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EmbeddedKafkaBroker broker;

    @Test
    @DisplayName("user.events / UserApproved 수신 → SlackMessage 저장 + 처리 기록")
    void consume_user_approved() {
        kafkaTemplate.send("user.events",
                "{\"eventType\":\"UserApproved\",\"eventId\":\"it-1\",\"userName\":\"홍길동\"}");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Optional<SlackMessage> message = slackMessageRepository.findByIdempotencyKey("it-1");
            assertThat(message).isPresent();
            assertThat(message.get().getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(processedEventRepository.existsByEventIdAndConsumerGroup("it-1", GROUP)).isTrue();
        });
    }

    @Test
    @DisplayName("동일 eventId 중복 발행 → SlackMessage 1건만 (멱등)")
    void idempotent_duplicate() {
        String message =
                "{\"eventType\":\"OrderCanceled\",\"eventId\":\"it-dup\",\"orderId\":\"O-1\",\"reason\":\"재고\"}";
        kafkaTemplate.send("order.events", message);
        kafkaTemplate.send("order.events", message);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(processedEventRepository.existsByEventIdAndConsumerGroup("it-dup", GROUP)).isTrue());

        assertThat(slackMessageRepository.findByIdempotencyKey("it-dup")).isPresent();
        assertThat(slackMessageRepository.findAll().stream()
                .filter(m -> "it-dup".equals(m.getIdempotencyKey()))
                .count()).isEqualTo(1);
    }

    @Test
    @DisplayName("delivery-manager.events / DeliveryAssigned 수신 → 발송시한 예측 후 SlackMessage 저장")
    void consume_delivery_assigned() {
        kafkaTemplate.send("delivery-manager.events", """
                {
                  "eventType":"DeliveryAssigned","eventId":"it-da","deliveryId":"DLV-1001",
                  "order":{"orderId":"ORD-1234","products":[{"name":"고등어","quantity":10}],
                           "requesterNote":"냉장","requestedDeadline":"2026-06-01T18:00:00+09:00"},
                  "route":{"origin":"서울 중부센터","waypoints":["대전허브"],"destination":"부산 해운대구"},
                  "totalEstimatedDurationSeconds":10800,
                  "agent":{"agentId":"AGT-7","name":"김배송","workingHours":{"start":"09:00","end":"18:00"}}
                }
                """);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Optional<SlackMessage> message = slackMessageRepository.findByIdempotencyKey("it-da");
            assertThat(message).isPresent();
            // 예측(FakeLlm)→템플릿 렌더→발송까지 완주 검증
            assertThat(message.get().getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(processedEventRepository.existsByEventIdAndConsumerGroup("it-da", GROUP)).isTrue();
        });
    }

    @Test
    @DisplayName("delivery-manager.events / 상세 없는 이벤트 → 더미로 채워 발송 완주 (임시)")
    void consume_delivery_assigned_thin() {
        kafkaTemplate.send("delivery-manager.events",
                "{\"eventType\":\"DeliveryAssigned\",\"eventId\":\"it-da-thin\"}");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Optional<SlackMessage> message = slackMessageRepository.findByIdempotencyKey("it-da-thin");
            assertThat(message).isPresent();
            // order/route/agent 누락 → withDummyDefaults가 채워 예측→발송까지 완주
            assertThat(message.get().getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(processedEventRepository.existsByEventIdAndConsumerGroup("it-da-thin", GROUP)).isTrue();
        });
    }

    @Test
    @DisplayName("파싱 불가 메시지 → 재시도 후 notification.dlq 이관")
    void invalid_payload_to_dlq() {
        try (Consumer<String, String> dlqConsumer = createConsumer()) {
            kafkaTemplate.send("user.events", "not-json");

            ConsumerRecord<String, String> dlqRecord =
                    KafkaTestUtils.getSingleRecord(dlqConsumer, "notification.dlq", Duration.ofSeconds(15));

            assertThat(dlqRecord.value()).isEqualTo("not-json");
        }
    }

    private Consumer<String, String> createConsumer() {
        Map<String, Object> props = KafkaTestUtils.consumerProps("dlq-test-group", "true", broker);
        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new StringDeserializer()).createConsumer();
        consumer.subscribe(List.of("notification.dlq"));
        return consumer;
    }
}
