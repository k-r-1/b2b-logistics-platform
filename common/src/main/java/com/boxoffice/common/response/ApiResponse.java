package com.boxoffice.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * 모든 API 응답의 공통 래퍼 클래스.
 * 응답 형식을 { status, message, data } 또는 { status, message, errors } 로 통일한다.
 * null 필드는 JSON 직렬화 시 생략한다.
 *
 * 사용 예시:
 *   성공 (200): ApiResponse.success(data)
 *   성공 (201): ApiResponse.success(HttpStatus.CREATED, data)
 *   실패:       ApiResponse.error(404, "HUB-001")
 *   검증 실패:  ApiResponse.error(400, "VALIDATION_ERROR", errors)
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** HTTP 상태 코드 */
    private final int status;

    /** 응답 메시지. 성공 시 "SUCCESS", 실패 시 에러 코드. */
    private final String message;

    /** 응답 데이터. null이면 JSON에서 생략됨. */
    private final T data;

    /** 검증 오류 목록. @Valid 실패 시 필드별 오류 메시지. null이면 생략됨. */
    private final List<String> errors;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(HttpStatus.OK.value(), "SUCCESS", data, null);
    }

    public static <T> ApiResponse<T> success(HttpStatus status, T data) {
        return new ApiResponse<>(status.value(), "SUCCESS", data, null);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(HttpStatus.OK.value(), "SUCCESS", null, null);
    }

    public static <T> ApiResponse<T> error(int status, String errorMessage) {
        return new ApiResponse<>(status, errorMessage, null, null);
    }

    public static <T> ApiResponse<T> error(int status, String errorMessage, List<String> errors) {
        return new ApiResponse<>(status, errorMessage, null, errors);
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String errorMessage) {
        return new ApiResponse<>(status.value(), errorMessage, null, null);
    }
}