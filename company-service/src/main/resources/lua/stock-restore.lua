-- 재고 원자적 복원 (주문 취소 보상). 차감과 동일하게 스크립트 전체가 원자 실행된다.
-- KEYS[1] 복원 멱등성 키, KEYS[2] 차감 멱등성 키, KEYS[3..] 상품별 재고 키
-- ARGV[1] TTL초, ARGV[2..n+1] 복원 수량
-- 반환: 0 성공, -1 중복 복원, -2 차감 이력 없음

if redis.call('EXISTS', KEYS[2]) == 0 then
    return -2
end
if redis.call('EXISTS', KEYS[1]) == 1 then
    return -1
end

local n = #KEYS - 2
for i = 1, n do
    redis.call('INCRBY', KEYS[i + 2], tonumber(ARGV[1 + i]))
end

redis.call('SET', KEYS[1], '1', 'EX', tonumber(ARGV[1]))
return 0
