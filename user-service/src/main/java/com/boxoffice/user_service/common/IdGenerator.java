package com.boxoffice.user_service.common;

import com.fasterxml.uuid.Generators;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class IdGenerator {

    /**
     * java-uuid-generator를 사용하여 시간 기반의 순차 정렬형 UUID v7을 생성합니다.
     */
    public static UUID createUUIDv7() {
        return Generators.timeBasedEpochGenerator().generate();
    }
}