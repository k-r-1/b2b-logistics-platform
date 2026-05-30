package boxoffice.orderservice.benchmark;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * JMH 벤치마크 실행 진입점.
 *
 * 실행 방법:
 *   ./gradlew test --tests "boxoffice.orderservice.benchmark.BenchmarkRunner"
 *
 * 결과 파일: build/reports/benchmarks/cache-benchmark.json
 *   → https://jmh.morethan.io 에서 시각화 가능
 */
@Tag("benchmark")
class BenchmarkRunner {

    @Disabled("CI 빌드에서 벤치마크는 제외")
    @Test
    void runUpdateOrderCacheBenchmark() throws Exception {
        Options opt = new OptionsBuilder()
            .include(UpdateOrderCacheBenchmark.class.getSimpleName())
            .forks(1)
            .resultFormat(ResultFormatType.JSON)
            .result("build/reports/benchmarks/cache-benchmark.json")
            .build();

        new Runner(opt).run();
    }
}
