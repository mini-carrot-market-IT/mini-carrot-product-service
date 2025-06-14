-- 데이터베이스 생성 및 사용
USE mini_carrot_product;

-- 사용자 테이블
CREATE TABLE IF NOT EXISTS carrot_users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    nickname VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_nickname (nickname)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 상품 테이블
CREATE TABLE IF NOT EXISTS carrot_products (
    product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    price INT NOT NULL,
    category VARCHAR(100) NOT NULL,
    image_url VARCHAR(500),
    seller_id BIGINT NOT NULL,
    seller_nickname VARCHAR(100) NOT NULL,
    status ENUM('AVAILABLE', 'SOLD', 'RESERVED') DEFAULT 'AVAILABLE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_seller_id (seller_id),
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (seller_id) REFERENCES carrot_users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 구매 테이블
CREATE TABLE IF NOT EXISTS carrot_purchases (
    purchase_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    purchase_price INT NOT NULL,
    purchase_status ENUM('PENDING', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING',
    purchased_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_product_id (product_id),
    INDEX idx_buyer_id (buyer_id),
    INDEX idx_seller_id (seller_id),
    INDEX idx_purchased_at (purchased_at),
    FOREIGN KEY (user_id) REFERENCES carrot_users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES carrot_products(product_id) ON DELETE CASCADE,
    FOREIGN KEY (buyer_id) REFERENCES carrot_users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (seller_id) REFERENCES carrot_users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 관심 상품 테이블 (찜하기)
CREATE TABLE IF NOT EXISTS carrot_favorites (
    favorite_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_product (user_id, product_id),
    INDEX idx_user_id (user_id),
    INDEX idx_product_id (product_id),
    FOREIGN KEY (user_id) REFERENCES carrot_users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES carrot_products(product_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 분석 이벤트 테이블 (선택사항 - 실제 분석 데이터 저장용)
CREATE TABLE IF NOT EXISTS carrot_analytics_events (
    event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    user_id BIGINT,
    product_id BIGINT,
    session_id VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,
    event_data JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_event_type (event_type),
    INDEX idx_user_id (user_id),
    INDEX idx_product_id (product_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 알림 테이블
CREATE TABLE IF NOT EXISTS carrot_notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type ENUM('PUSH', 'EMAIL', 'SMS') DEFAULT 'PUSH',
    is_read BOOLEAN DEFAULT FALSE,
    related_product_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES carrot_users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (related_product_id) REFERENCES carrot_products(product_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 샘플 사용자 데이터 삽입
INSERT INTO carrot_users (email, nickname, password) VALUES
('test1@example.com', '판매왕김씨', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFr4jNMpOgTCqfSqhDqBe'), -- password: test123
('test2@example.com', '구매요정', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFr4jNMpOgTCqfSqhDqBe'),
('test3@example.com', '중고마니아', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFr4jNMpOgTCqfSqhDqBe'),
('admin@minicarrot.com', '관리자', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFr4jNMpOgTCqfSqhDqBe');

-- 샘플 상품 데이터 삽입
INSERT INTO carrot_products (title, description, price, category, image_url, seller_id, seller_nickname, status) VALUES
('아이폰 14 Pro 256GB', '거의 새상품입니다. 케이스 끼고 사용해서 스크래치 없어요!', 1200000, 'electronics', '/uploads/iphone14.jpg', 1, '판매왕김씨', 'AVAILABLE'),
('나이키 에어맥스 270', '사이즈 270 신발입니다. 몇 번 안 신어서 상태 좋아요', 120000, 'fashion', '/uploads/nike270.jpg', 1, '판매왕김씨', 'AVAILABLE'),
('맥북 에어 M2', '2023년 구입, 보증서 있습니다. 대학생이라 가격 좀 봐주세요ㅠㅠ', 1500000, 'electronics', '/uploads/macbook.jpg', 2, '구매요정', 'AVAILABLE'),
('무선 청소기', 'LG 코드제로 A9S 모델입니다. 집이 좁아서 팔아요', 180000, 'home', '/uploads/vacuum.jpg', 3, '중고마니아', 'SOLD'),
('플레이스테이션 5', '거의 안 했어요. 게임 3개 포함해서 드려요', 650000, 'electronics', '/uploads/ps5.jpg', 1, '판매왕김씨', 'RESERVED'),
('원목 책상', '이케아에서 산 원목 책상입니다. 직거래만 가능해요', 80000, 'furniture', '/uploads/desk.jpg', 2, '구매요정', 'AVAILABLE'),
('캐논 DSLR 카메라', 'EOS 200D 렌즈 포함입니다. 사진 취미 그만둬서 팔아요', 420000, 'electronics', '/uploads/camera.jpg', 3, '중고마니아', 'AVAILABLE'),
('겨울 패딩 자켓', '노스페이스 700 필파워 다운자켓, 사이즈 M', 150000, 'fashion', '/uploads/padding.jpg', 1, '판매왕김씨', 'AVAILABLE');

-- 샘플 구매 데이터
INSERT INTO carrot_purchases (user_id, product_id, buyer_id, seller_id, purchase_price, purchase_status) VALUES
(2, 4, 2, 3, 180000, 'COMPLETED'),
(3, 5, 3, 1, 650000, 'PENDING');

-- 샘플 관심 상품 데이터
INSERT INTO carrot_favorites (user_id, product_id) VALUES
(2, 1), (2, 3), (2, 7),
(3, 1), (3, 2), (3, 6),
(1, 3), (1, 6);

-- 샘플 알림 데이터
INSERT INTO carrot_notifications (user_id, title, message, type, related_product_id) VALUES
(1, '상품이 판매되었습니다!', '무선 청소기 상품을 구매요정님이 구매했습니다.', 'PUSH', 4),
(2, '구매가 완료되었습니다!', '무선 청소기 구매가 완료되었습니다.', 'EMAIL', 4),
(3, '새로운 상품이 등록되었습니다!', '근처에 겨울 패딩 자켓이 등록되었습니다.', 'PUSH', 8);

-- 성능을 위한 추가 인덱스
CREATE INDEX idx_products_price ON carrot_products(price);
CREATE INDEX idx_products_title ON carrot_products(title);
CREATE INDEX idx_products_category_status ON carrot_products(category, status);
CREATE INDEX idx_purchases_status ON carrot_purchases(purchase_status);

-- 뷰 생성 (자주 사용하는 쿼리 최적화)
CREATE VIEW v_product_with_favorites AS
SELECT 
    p.*,
    COUNT(f.favorite_id) as favorite_count
FROM carrot_products p
LEFT JOIN carrot_favorites f ON p.product_id = f.product_id
GROUP BY p.product_id;

CREATE VIEW v_user_stats AS
SELECT 
    u.user_id,
    u.nickname,
    COUNT(DISTINCT p.product_id) as products_count,
    COUNT(DISTINCT pur.purchase_id) as purchases_count,
    COUNT(DISTINCT f.favorite_id) as favorites_count
FROM carrot_users u
LEFT JOIN carrot_products p ON u.user_id = p.seller_id
LEFT JOIN carrot_purchases pur ON u.user_id = pur.buyer_id
LEFT JOIN carrot_favorites f ON u.user_id = f.user_id
GROUP BY u.user_id, u.nickname;

-- 완료 메시지
SELECT 'Database initialization completed successfully!' as message; 