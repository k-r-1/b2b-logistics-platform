-- SearchOrders 성능 최적화를 위한 Partial Composite Index
-- Flyway 미사용 환경에서 DB 초기화 후 수동 실행 필요
-- 적용 환경: PostgreSQL (ddl-auto: none 또는 validate 전환 후)

-- SUPPLIER_MANAGER 쿼리: supplierId OR receiverId + 정렬
CREATE INDEX IF NOT EXISTS idx_order_supplier_created_partial
  ON p_orders (supplier_id, created_at DESC)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_order_receiver_created_partial
  ON p_orders (receiver_id, created_at DESC)
  WHERE deleted_at IS NULL;

-- HUB_MANAGER 쿼리: sourceHubId OR destinationHubId + 정렬
CREATE INDEX IF NOT EXISTS idx_order_source_hub_created_partial
  ON p_orders (source_hub_id, created_at DESC)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_order_dest_hub_created_partial
  ON p_orders (destination_hub_id, created_at DESC)
  WHERE deleted_at IS NULL;

-- status + created_at 복합 인덱스 (날짜 범위 + 상태 복합 조회용)
CREATE INDEX IF NOT EXISTS idx_order_status_created_partial
  ON p_orders (status, created_at DESC)
  WHERE deleted_at IS NULL;

-- 적용 효과 확인 (EXPLAIN ANALYZE 사용)
-- EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
-- SELECT * FROM p_orders
-- WHERE source_hub_id = '<uuid>' OR destination_hub_id = '<uuid>'
-- AND deleted_at IS NULL
-- ORDER BY created_at DESC
-- LIMIT 10 OFFSET 0;
