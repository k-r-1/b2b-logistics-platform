package com.boxoffice.ainotificationservice.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.boxoffice.ainotificationservice.notification.entity.message.EventCause;
import com.boxoffice.ainotificationservice.notification.entity.message.NotificationStatus;
import com.boxoffice.ainotificationservice.notification.entity.message.Recipient;
import com.boxoffice.ainotificationservice.notification.entity.message.SlackMessage;
import com.boxoffice.ainotificationservice.notification.entity.message.TemplateType;
import com.boxoffice.ainotificationservice.notification.exception.NotificationErrorCode;
import com.boxoffice.ainotificationservice.notification.repository.SlackMessageRepository;
import com.boxoffice.ainotificationservice.notification.template.OrderCanceledContext;
import com.boxoffice.ainotificationservice.notification.template.TemplateContext;
import com.boxoffice.ainotificationservice.notification.template.TemplateRenderer;
import com.boxoffice.ainotificationservice.notification.template.UserApprovedContext;
import com.boxoffice.common.exception.BaseException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@DisplayName("NotificationService")
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private SlackMessageRepository repository;

    @Mock
    private TemplateRenderer renderer;

    @Mock
    private ApplicationEventPublisher publisher;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository, renderer, publisher);
    }

    @Nested
    @DisplayName("sendDirect()")
    class SendDirect {

        @Test
        @DisplayName("멱등성 키 중복 - 기존 반환, 렌더링/저장/이벤트 발행 호출 X")
        void duplicate_idempotency_key_returns_existing_and_skips() {
            // given
            Recipient recipient = Recipient.user("U1");
            TemplateContext context = new UserApprovedContext("홍길동");
            SlackMessage existing = SlackMessage.direct(
                    "idem-1", recipient, TemplateType.USER_APPROVED, "기존 본문");
            given(repository.findByIdempotencyKey("idem-1")).willReturn(Optional.of(existing));

            // when
            SlackMessage result = service.sendDirect("idem-1", recipient, context);

            // then
            assertThat(result).isSameAs(existing);
            then(renderer).should(never()).render(any());
            then(repository).should(never()).save(any());
            then(publisher).should(never()).publishEvent(any());
        }

        @Test
        @DisplayName("정상 - PENDING 저장 + SlackMessageQueuedEvent 발행 + cause 없음")
        void enqueue_pending_and_publish_event() {
            // given
            Recipient recipient = Recipient.user("U1");
            TemplateContext context = new UserApprovedContext("홍길동");
            given(repository.findByIdempotencyKey("idem-1")).willReturn(Optional.empty());
            given(renderer.render(context)).willReturn("안녕하세요 홍길동님");

            // when
            SlackMessage result = service.sendDirect("idem-1", recipient, context);

            // then
            assertThat(result.getStatus()).isEqualTo(NotificationStatus.PENDING);
            assertThat(result.getAttemptCount()).isZero();
            assertThat(result.cause()).isEmpty();
            then(repository).should().save(any(SlackMessage.class));
            then(publisher).should().publishEvent(any(SlackMessageQueuedEvent.class));
        }
    }

    @Nested
    @DisplayName("sendFromEvent()")
    class SendFromEvent {

        @Test
        @DisplayName("정상 - PENDING 저장 + cause 보존 + 이벤트 발행")
        void enqueue_with_cause() {
            // given
            Recipient recipient = Recipient.channel("C1");
            TemplateContext context = new OrderCanceledContext("ORD-1", "재고 부족");
            EventCause cause = new EventCause("evt-100", "order-service");
            given(repository.findByIdempotencyKey("idem-2")).willReturn(Optional.empty());
            given(renderer.render(context)).willReturn("주문 #ORD-1 취소");

            // when
            SlackMessage result = service.sendFromEvent("idem-2", recipient, context, cause);

            // then
            assertThat(result.getStatus()).isEqualTo(NotificationStatus.PENDING);
            assertThat(result.cause()).contains(cause);
            then(repository).should().save(any(SlackMessage.class));
            then(publisher).should().publishEvent(any(SlackMessageQueuedEvent.class));
        }

        @Test
        @DisplayName("실패 - null cause는 fromEvent 도메인 검증에서 차단")
        void fail_null_cause() {
            // given
            Recipient recipient = Recipient.channel("C1");
            TemplateContext context = new OrderCanceledContext("ORD-1", "재고 부족");
            given(repository.findByIdempotencyKey("idem-2")).willReturn(Optional.empty());
            given(renderer.render(context)).willReturn("주문 #ORD-1 취소");

            // when & then
            assertThatThrownBy(() -> service.sendFromEvent("idem-2", recipient, context, null))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(NotificationErrorCode.INVALID_EVENT_CAUSE);
        }
    }
}
