package com.boxoffice.ainotificationservice.notification.client;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;

import java.time.Duration;
import java.util.Optional;

public record SendResult(
        boolean success,
        Integer httpStatus,
        FailureType failureType,
        String errorMessage,
        Duration latency
) {

    public SendResult {
        if (latency == null || latency.isNegative()) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
        if (!success && failureType == null) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
        if (success && failureType != null) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
    }

    public static SendResult success(int httpStatus, Duration latency) {
        return new SendResult(true, httpStatus, null, null, latency);
    }

    public static SendResult permanentFailure(Integer httpStatus, String errorMessage, Duration latency) {
        return new SendResult(false, httpStatus, FailureType.PERMANENT, errorMessage, latency);
    }

    public static SendResult transientFailure(Integer httpStatus, String errorMessage, Duration latency) {
        return new SendResult(false, httpStatus, FailureType.TRANSIENT, errorMessage, latency);
    }

    public Optional<Integer> httpStatusOptional() {
        return Optional.ofNullable(httpStatus);
    }

    public Optional<FailureType> failureTypeOptional() {
        return Optional.ofNullable(failureType);
    }

    public Optional<String> errorMessageOptional() {
        return Optional.ofNullable(errorMessage);
    }
}
