package com.boxoffice.ainotificationservice.notification.service;

import com.boxoffice.ainotificationservice.notification.entity.message.Recipient;

// 트랜잭션 안에서 추출해 외부 호출에 전달하는 발송 데이터. 영속성 컨텍스트 의존 X.
public record DispatchSnapshot(Recipient recipient, String body) {

}
