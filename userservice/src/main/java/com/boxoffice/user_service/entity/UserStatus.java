package com.boxoffice.user_service.entity;

public enum UserStatus {
    PENDING,   // 가입 승인 대기 (회원가입 시 기본값)
    APPROVED,  // 승인 완료 (로그인 가능)
    REJECTED   // 승인 거절
}