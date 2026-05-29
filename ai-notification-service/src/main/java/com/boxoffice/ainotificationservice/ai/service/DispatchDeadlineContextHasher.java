package com.boxoffice.ainotificationservice.ai.service;

import com.boxoffice.ainotificationservice.ai.deadline.DispatchDeadlineContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

// LLM 입력을 결정론적 문자열로 정규화 후 SHA-256 해시. 같은 입력 → 같은 키(캐시 조회·로그 추적용).
@Component
public class DispatchDeadlineContextHasher {

    public String hash(DispatchDeadlineContext context) {
        byte[] digest = sha256(canonicalize(context));
        return HexFormat.of().formatHex(digest);
    }

    private String canonicalize(DispatchDeadlineContext context) {
        String products = context.products().stream()
                .map(line -> line.productName() + ":" + line.quantity())
                .collect(Collectors.joining(","));
        String waypoints = String.join(",", context.route().waypoints());
        return String.join("|",
                context.requestedDeadline().toString(),
                context.requesterNoteOptional().orElse(""),
                products,
                context.route().origin(),
                waypoints,
                context.route().destination(),
                Long.toString(context.totalEstimatedDuration().toSeconds()),
                context.agentWorkingHours().start() + "-" + context.agentWorkingHours().end());
    }

    private byte[] sha256(String input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 모든 JVM이 보장하는 표준 알고리즘 — 도달 불가
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
