package com.boxoffice.common.exception;

import com.boxoffice.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Optional;

/**
 * 전역 예외 처리 핸들러.
 *
 * 모든 서비스의 Controller에서 발생하는 예외를 처리하여
 * ApiResponse 형식으로 통일된 응답을 반환한다.
 *
 * 처리 대상:
 *   BaseException: 모든 도메인 예외 (HubErrorCode, OrderErrorCode 등)
 *   MethodArgumentNotValidException: @Valid 검증 실패
 *   FeignException: 서비스 간 FeignClient 호출 실패
 *   Exception: 예상치 못한 모든 예외
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 모든 도메인 예외를 처리한다.
     * BaseException을 상속한 모든 예외를 처리한다.
     * ErrorCode 인터페이스 기반으로 어떤 서비스 에러든 동일하게 처리.
     *
     * @param e 발생한 예외
     * @return 에러 코드와 HTTP 상태 메시지가 담긴 응답
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("[BaseException] code: {}, message: {}", errorCode.getCode(), e.getMessage());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(
                        errorCode.getHttpStatus().value(),
                        errorCode.getMessage()
                ));
    }

    /**
     * @Valid 검증 실패를 처리한다.
     * 필드별 검증 오류 메시지를 errors 배열로 반환한다.
     *
     * @param e 검증 실패 예외
     * @return errors 배열이 담긴 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult().getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        log.warn("[ValidationException] {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        HttpStatus.BAD_REQUEST.value(),
                        "VALIDATION_ERROR",
                        errors
                ));
    }

    /**
     * FeignClient 호출 실패를 처리한다.
     *
     * @param e FeignClient 예외
     * @return 서비스 간 호출 실패 응답
     */
    @ExceptionHandler(feign.FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeignException(feign.FeignException e) {
        log.error("[FeignException] status: {}, message: {}", e.status(), e.getMessage());
        HttpStatus status = Optional.ofNullable(HttpStatus.resolve(e.status()))
                .orElse(HttpStatus.INTERNAL_SERVER_ERROR);
        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(
                        status.value(),
                        CommonErrorCode.FEIGN_CLIENT_ERROR.getMessage()
                ));
    }

    /**
     * 예상치 못한 모든 예외를 처리한다.
     *
     * @param e 발생한 예외
     * @return 서버 내부 오류 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("[UnhandledException] {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage()
                ));
    }
}