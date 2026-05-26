package com.boxoffice.deliverymanagerservice.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ManagerStatus {

    WAITING("대기 중"),      // 배송 지시를 받을 수 있는 상태
    DELIVERING("배송 중"),   // 현재 물건을 배송하고 있는 상태
    BREAK("휴식 중"),        // 식사 또는 휴식 상태
    OFF("퇴근");             // 업무 종료 상태

    private final String description;
}