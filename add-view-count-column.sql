-- 조회수 컬럼 추가
ALTER TABLE carrot_products ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0;

-- 기존 데이터의 조회수를 0으로 초기화 (이미 DEFAULT 0으로 설정됨)
UPDATE carrot_products SET view_count = 0 WHERE view_count IS NULL;

-- 인덱스 추가 (조회수 기반 정렬을 위해)
CREATE INDEX idx_carrot_products_view_count ON carrot_products(view_count DESC);

-- 복합 인덱스 추가 (카테고리별 조회수 정렬을 위해)
CREATE INDEX idx_carrot_products_category_view_count ON carrot_products(category, view_count DESC); 