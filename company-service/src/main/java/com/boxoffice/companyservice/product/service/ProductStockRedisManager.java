package com.boxoffice.companyservice.product.service;

import com.boxoffice.companyservice.product.entity.ProductStockOperationType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 재고 차감/복원을 Redis Lua 스크립트로 원자 처리한다.
 * 락 없이 "확인 → 차감 → 멱등성 기록"을 한 번에 끝내 초과 판매를 막는다.
 * (재고 원천은 DB, Redis는 빠른 처리용 카운터로 시드해서 사용)
 */
@Component
public class ProductStockRedisManager {

    private static final String KEY_PREFIX = "company-service:product-stock";
    private static final long DONE_TTL_SECONDS = Duration.ofDays(7).toSeconds();

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> deductScript;
    private final RedisScript<Long> restoreScript;

    public ProductStockRedisManager(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.deductScript = loadScript("lua/stock-deduct.lua");
        this.restoreScript = loadScript("lua/stock-restore.lua");
    }

    private RedisScript<Long> loadScript(String location) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(location));
        script.setResultType(Long.class);
        return script;
    }

    public enum DeductStatus {
        SUCCESS,
        ALREADY_DONE,   // 중복 주문 → 재고 변화 없음
        INSUFFICIENT
    }

    public record DeductResult(DeductStatus status, UUID insufficientProductId) {
        static DeductResult success() {
            return new DeductResult(DeductStatus.SUCCESS, null);
        }

        static DeductResult alreadyDone() {
            return new DeductResult(DeductStatus.ALREADY_DONE, null);
        }

        static DeductResult insufficient(UUID productId) {
            return new DeductResult(DeductStatus.INSUFFICIENT, productId);
        }
    }

    public enum RestoreStatus {
        SUCCESS,
        ALREADY_DONE,
        NO_DEDUCT_HISTORY
    }

    /**
     * 여러 상품 재고를 원자적으로 차감한다.
     * productIds는 데드락 방지를 위해 정렬해서 전달하고, quantities/dbStocks는 같은 순서를 맞춘다.
     */
    public DeductResult deduct(UUID orderId, List<UUID> productIds, List<Integer> quantities, List<Integer> dbStocks) {
        List<String> keys = new ArrayList<>();
        keys.add(doneKey(orderId, ProductStockOperationType.DEDUCT));
        for (UUID productId : productIds) {
            keys.add(stockKey(productId));
        }

        List<String> args = new ArrayList<>();
        args.add(String.valueOf(DONE_TTL_SECONDS));
        for (Integer quantity : quantities) {
            args.add(String.valueOf(quantity));
        }
        for (Integer dbStock : dbStocks) {
            args.add(String.valueOf(dbStock));
        }

        long code = execute(deductScript, keys, args);
        if (code == 0) {
            return DeductResult.success();
        }
        if (code == -1) {
            return DeductResult.alreadyDone();
        }
        if (code >= 1) {
            return DeductResult.insufficient(productIds.get((int) code - 1));
        }
        throw new IllegalStateException("알 수 없는 재고 차감 결과: " + code);
    }

    /**
     * 여러 상품 재고를 원자적으로 복원한다 (주문 취소 보상).
     */
    public RestoreStatus restore(UUID orderId, List<UUID> productIds, List<Integer> quantities) {
        List<String> keys = new ArrayList<>();
        keys.add(doneKey(orderId, ProductStockOperationType.RESTORE));
        keys.add(doneKey(orderId, ProductStockOperationType.DEDUCT));
        for (UUID productId : productIds) {
            keys.add(stockKey(productId));
        }

        List<String> args = new ArrayList<>();
        args.add(String.valueOf(DONE_TTL_SECONDS));
        for (Integer quantity : quantities) {
            args.add(String.valueOf(quantity));
        }

        long code = execute(restoreScript, keys, args);
        if (code == 0) {
            return RestoreStatus.SUCCESS;
        }
        if (code == -1) {
            return RestoreStatus.ALREADY_DONE;
        }
        if (code == -2) {
            return RestoreStatus.NO_DEDUCT_HISTORY;
        }
        throw new IllegalStateException("알 수 없는 재고 복원 결과: " + code);
    }

    private long execute(RedisScript<Long> script, List<String> keys, List<String> args) {
        Long result = redisTemplate.execute(script, keys, args.toArray());
        if (result == null) {
            throw new IllegalStateException("재고 Lua 스크립트가 null을 반환했습니다.");
        }
        return result;
    }

    private String stockKey(UUID productId) {
        return KEY_PREFIX + ":stock:" + productId;
    }

    private String doneKey(UUID orderId, ProductStockOperationType operationType) {
        return KEY_PREFIX + ":done:" + operationType.name().toLowerCase() + ":" + orderId;
    }
}
