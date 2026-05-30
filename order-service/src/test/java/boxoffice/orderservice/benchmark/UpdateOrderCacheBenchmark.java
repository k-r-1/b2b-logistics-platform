package boxoffice.orderservice.benchmark;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * updateOrder의 캐시 유무 성능 비교 벤치마크.
 *
 * 측정 대상 지연 모델 (일반적인 로컬/스테이징 환경 기준):
 *   DB  read/write : 5 ms
 *   Redis GET      : 1 ms
 *   Redis DEL      : 1 ms
 *
 * 시나리오:
 *   1. read_cacheHit            – 이미 캐시에 올라와 있는 상태의 단건 읽기
 *   2. read_noCache             – 캐시 없이 매번 DB 읽기
 *   3. updateThenRead_withCache – 수정(@CacheEvict) 후 즉시 1회 읽기
 *   4. updateThenRead_noCache   – 수정 후 즉시 1회 읽기 (캐시 없음)
 *   5. readHeavy_withCache      – 1 수정 + 10 읽기 (읽기-다중 워크로드)
 *   6. readHeavy_noCache        – 1 수정 + 10 읽기 (캐시 없음)
 *
 * 예상 결과 (ms, 단일 스레드 기준):
 *   read_cacheHit            ≈  1   (Redis GET 1회)
 *   read_noCache             ≈  5   (DB 1회)
 *   updateThenRead_withCache ≈ 11   (DB5 + DEL1 + DB5)   ← 단발 읽기에선 불리
 *   updateThenRead_noCache   ≈ 10   (DB5 + DB5)
 *   readHeavy_withCache      ≈ 20   (DB5+DEL1+DB5 + 9×1) ← 읽기 다수일 때 유리
 *   readHeavy_noCache        ≈ 55   (DB5 + 10×DB5)
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class UpdateOrderCacheBenchmark {

    private static final long DB_MS = 5L;
    private static final long REDIS_MS = 1L;
    private static final int N_READS = 10;

    private final ConcurrentHashMap<UUID, String> cache = new ConcurrentHashMap<>();
    private UUID orderId;

    @Setup(Level.Trial)
    public void setup() {
        orderId = UUID.randomUUID();
    }

    // ── 시나리오 1: 캐시 히트 단건 읽기 ────────────────────────────────────────

    /**
     * @Cacheable 히트: 캐시에 이미 올라온 주문을 읽는 경우.
     * 매 Iteration 시작 전 캐시를 프리-워밍하여 순수 히트 시간만 측정.
     */
    @Setup(Level.Iteration)
    public void preWarmCache() {
        cache.put(orderId, "order:" + orderId);
    }

    @Benchmark
    public String read_cacheHit() throws InterruptedException {
        String value = cache.get(orderId);
        Thread.sleep(REDIS_MS); // Redis GET
        return value;
    }

    // ── 시나리오 2: 캐시 없이 매번 DB 읽기 ────────────────────────────────────

    @Benchmark
    public String read_noCache() throws InterruptedException {
        Thread.sleep(DB_MS); // DB SELECT
        return "order:" + orderId;
    }

    // ── 시나리오 3: 수정 후 즉시 1회 읽기 – 캐시 있음 ────────────────────────

    /**
     * updateOrder(@CacheEvict) → findByIdAsDto(@Cacheable) 1회.
     * 수정 직후 캐시 미스 → DB 재조회 비용 포함.
     * 단발성 업데이트+읽기에서는 캐시가 오히려 약간 느림.
     */
    @Benchmark
    public String updateThenRead_withCache() throws InterruptedException {
        // update: DB write + @CacheEvict (Redis DEL)
        Thread.sleep(DB_MS + REDIS_MS);
        cache.remove(orderId);

        // 즉시 read: 캐시 미스 → DB → 캐시 적재
        String cached = cache.get(orderId);
        if (cached != null) {
            Thread.sleep(REDIS_MS);
            return cached;
        }
        Thread.sleep(DB_MS);
        String result = "order_updated:" + orderId;
        cache.put(orderId, result);
        return result;
    }

    // ── 시나리오 4: 수정 후 즉시 1회 읽기 – 캐시 없음 ───────────────────────

    @Benchmark
    public String updateThenRead_noCache() throws InterruptedException {
        Thread.sleep(DB_MS); // DB write
        Thread.sleep(DB_MS); // DB read
        return "order_updated:" + orderId;
    }

    // ── 시나리오 5: 읽기 다중 워크로드 – 캐시 있음 ────────────────────────────

    /**
     * 1번 업데이트 후 N번 읽기: 캐시의 진짜 효과가 드러나는 시나리오.
     * 두 번째 읽기부터는 Redis 히트(1ms)로 응답 → 누적 절감 효과 극대화.
     * 예상: DB5 + DEL1 + DB5(미스) + 9×Redis1 ≈ 20 ms
     */
    @Benchmark
    public void readHeavy_withCache() throws InterruptedException {
        // 1 update
        Thread.sleep(DB_MS + REDIS_MS);
        cache.remove(orderId);

        // N reads
        for (int i = 0; i < N_READS; i++) {
            String cached = cache.get(orderId);
            if (cached != null) {
                Thread.sleep(REDIS_MS); // Redis GET
            } else {
                Thread.sleep(DB_MS);    // DB SELECT + cache 적재
                cache.put(orderId, "order_updated:" + orderId);
            }
        }
    }

    // ── 시나리오 6: 읽기 다중 워크로드 – 캐시 없음 ──────────────────────────

    /**
     * 예상: DB5(업데이트) + 10×DB5(읽기) ≈ 55 ms
     * readHeavy_withCache(≈20 ms)와 비교하면 약 2.75× 느림.
     */
    @Benchmark
    public void readHeavy_noCache() throws InterruptedException {
        Thread.sleep(DB_MS); // DB write
        for (int i = 0; i < N_READS; i++) {
            Thread.sleep(DB_MS); // DB SELECT (always)
        }
    }
}
