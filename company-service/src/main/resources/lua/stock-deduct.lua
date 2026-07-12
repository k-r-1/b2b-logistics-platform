-- 재고 원자적 차감. Redis는 싱글 스레드라 이 스크립트 전체가 원자 실행돼 락이 필요 없다.
-- KEYS[1] 멱등성 키(orderId), KEYS[2..] 상품별 재고 키
-- ARGV[1] TTL초, ARGV[2..n+1] 차감 수량, ARGV[n+2..] 초기 재고(시드용)
-- 반환: 0 성공, -1 중복 주문, i>0 i번째 상품 재고 부족

if redis.call('EXISTS', KEYS[1]) == 1 then
    return -1
end

local n = #KEYS - 1

-- 확인 단계: 하나라도 부족하면 차감하지 않는다
for i = 1, n do
    local stockKey = KEYS[i + 1]
    if redis.call('EXISTS', stockKey) == 0 then
        redis.call('SET', stockKey, ARGV[n + 1 + i]) -- 최초 1회 DB 값으로 시드
    end
    if tonumber(redis.call('GET', stockKey)) < tonumber(ARGV[1 + i]) then
        return i
    end
end

for i = 1, n do
    redis.call('DECRBY', KEYS[i + 1], tonumber(ARGV[1 + i]))
end

redis.call('SET', KEYS[1], '1', 'EX', tonumber(ARGV[1])) -- 차감과 함께 처리 완료 기록
return 0
