package com.boxoffice.hubservice.stocktransfer.entity;

public enum TransferStatus {
    /** 이전 요청 생성, 담당자 배정 대기 */
    PENDING,
    /** 담당자 출발, 이전 진행 중 */
    IN_PROGRESS,
    /** 이전 완료 */
    COMPLETED,
    /** 이전 취소 */
    CANCELLED
}
