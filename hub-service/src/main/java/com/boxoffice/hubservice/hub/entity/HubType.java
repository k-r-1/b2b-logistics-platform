package com.boxoffice.hubservice.hub.entity;

public enum HubType {
    /** 중앙 허브 - 경기남부, 대전, 대구 (3개) */
    CENTRAL,
    /** 일반 허브 - 각 지역 센터 (14개) */
    REGIONAL,
    /** 운영 중단 - 신규 배송 배정 불가 */
    INACTIVE
}
