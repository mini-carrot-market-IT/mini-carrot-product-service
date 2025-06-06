-- 더미 상품 데이터 삽입
-- static/image에 있는 이미지들에 맞는 실제 상품들

INSERT INTO carrot_products (
    title, description, price, category, image_url, seller_id, seller_nickname, status, created_at, updated_at
) VALUES 
-- 1. 햇반
('CJ 햇반 즉석밥 210g x 24개', 
 '신선한 쌀로 만든 CJ 햇반입니다. 전자레인지에 2분이면 갓 지은 밥처럼! 유통기한 넉넉하고 개당 1000원대로 저렴해요.',
 28000, '식품', '/uploads/햇반.webp', 1, '요리좋아하는사람', 'AVAILABLE',
 NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY),

-- 2. 모자
('빈티지 스냅백 모자',
 '깔끔한 디자인의 빈티지 스냅백 모자입니다. 남녀공용으로 착용 가능하며 거의 새 제품이에요. 원래 5만원에 샀는데 저렴하게 내놓습니다.',
 25000, '패션/의류', '/uploads/cap-1379590_1280.jpg', 2, '패션왕', 'AVAILABLE',
 NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),

-- 3. 운동기구
('홈트레이닝 덤벨 세트 (2kg x 2개)',
 '집에서 운동하기 좋은 덤벨 세트입니다. 2kg 짜리 2개로 초보자나 여성분들에게 적합해요. 고무 코팅되어 있어서 바닥 긁힘 없습니다.',
 35000, '스포츠/레저', '/uploads/fitness-equipment-2187127_1280.jpg', 3, '홈트족', 'AVAILABLE',
 NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 3 HOUR),

-- 4. 이어폰
('무선 블루투스 이어폰',
 '삼성/아이폰 호환 가능한 무선 이어폰입니다. 음질 좋고 배터리 오래 갑니다. 케이스랑 충전선 포함이고 사용감 거의 없어요!',
 45000, '전자기기', '/uploads/earphones-5064411_1280.jpg', 4, '음악매니아', 'AVAILABLE',
 NOW() - INTERVAL 5 HOUR, NOW() - INTERVAL 5 HOUR),

-- 5. 지갑
('천연가죽 장지갑 (브라운)',
 '정품 천연가죽으로 만든 고급 장지갑입니다. 카드 슬롯 많고 지폐, 동전 수납공간 넉넉해요. 선물받았는데 스타일이 안 맞아서 판매합니다.',
 80000, '패션/잡화', '/uploads/지갑-14-7565225_1280.jpg', 5, '가죽매니아', 'AVAILABLE',
 NOW() - INTERVAL 8 HOUR, NOW() - INTERVAL 8 HOUR),

-- 6. 아이폰
('아이폰 13 128GB (스페이스 그레이)',
 '아이폰 13 128GB 스페이스 그레이 색상입니다. 배터리 성능 92%로 양호하고 액정 깨짐 없어요. 케이스 끼고 써서 상태 좋습니다. 직거래 선호!',
 650000, '전자기기', '/uploads/iphone-7479302_1280.jpg', 6, '애플유저', 'AVAILABLE',
 NOW() - INTERVAL 12 HOUR, NOW() - INTERVAL 10 HOUR),

-- 7. 아기옷
('신생아 우주복 세트 (3-6개월)',
 '한 번도 안 입은 신생아 우주복 세트입니다. 사이즈가 안 맞아서 새 제품 그대로 판매해요. 면 100% 소재로 아기 피부에 안전합니다.',
 15000, '유아용품', '/uploads/baby-clothes-5749670_1280.jpg', 7, '신생아맘', 'AVAILABLE',
 NOW() - INTERVAL 15 HOUR, NOW() - INTERVAL 15 HOUR),

-- 8. 신발
('나이키 에어포스 1 (270mm)',
 '나이키 에어포스 1 화이트 색상입니다. 270mm 사이즈이고 몇 번 안 신어서 상태 좋아요. 정품이고 박스도 있습니다. 상급 택배 가능!',
 120000, '패션/신발', '/uploads/white-845071_1280.jpg', 8, '신발수집가', 'AVAILABLE',
 NOW() - INTERVAL 18 HOUR, NOW() - INTERVAL 18 HOUR),

-- 9. 노트북
('맥북 에어 M1 (8GB/256GB)',
 '맥북 에어 M1 모델입니다. 8GB 메모리, 256GB SSD로 사무용이나 학업용으로 충분해요. 충전기, 박스 포함이고 사용감 적습니다. 가격 협상 가능!',
 950000, '전자기기', '/uploads/computer-4795762_1280.jpg', 9, '맥북유저', 'AVAILABLE',
 NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY); 