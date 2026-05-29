package com.boxoffice.ainotificationservice.ai.entity.prediction;

import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlinePrediction;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// LLM 발송 시한 예측 결과 로그. INSERT-only(디버깅·재현용)라 BaseEntity 미상속, createdAt만 audit.
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "prediction_log", schema = "notification_schema")
public class PredictionLog {

    private static final TimeBasedEpochGenerator UUID_GENERATOR = Generators.timeBasedEpochGenerator();

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "input_hash", nullable = false, length = 64)
    private String inputHash;

    @Column(name = "dispatch_deadline", nullable = false)
    private LocalDateTime dispatchDeadline;

    @Column(name = "reasoning", columnDefinition = "TEXT")
    private String reasoning;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "fallback_used", nullable = false)
    private boolean fallbackUsed;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    private PredictionLog(String inputHash, DispatchDeadlinePrediction prediction) {
        if (inputHash == null || inputHash.isBlank() || prediction == null) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
        this.inputHash = inputHash;
        this.dispatchDeadline = prediction.dispatchDeadline();
        this.reasoning = prediction.reasoning();
        this.confidence = prediction.confidence();
        this.fallbackUsed = prediction.fallbackUsed();
    }

    public static PredictionLog of(String inputHash, DispatchDeadlinePrediction prediction) {
        return new PredictionLog(inputHash, prediction);
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID_GENERATOR.generate();
        }
    }

    public Optional<String> reasoningOptional() {
        return Optional.ofNullable(reasoning);
    }

    public Optional<Double> confidenceOptional() {
        return Optional.ofNullable(confidence);
    }
}
