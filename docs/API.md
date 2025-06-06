# Mini 당근마켓 Product Service API 문서

## 📦 Product Service API

### 🔵 상품 등록
**POST** `/api/products`

**Headers:**
```
Authorization: Bearer JWT_TOKEN
Content-Type: application/json
```

**Request Body:**
```json
{
  "title": "아이폰 14 프로 맥스",
  "description": "거의 새 제품, 케이스 포함",
  "price": 1200000,
  "category": "전자기기",
  "imageUrl": "/uploads/abc123.jpg"
}
```

**Response:**
```json
{
  "productId": 1,
  "status": "AVAILABLE"
}
```

---

### 🔵 상품 목록 조회
**GET** `/api/products`

**Query Parameters:**
- `category` (optional): 카테고리별 필터링

**Response:**
```json
[
  {
    "productId": 1,
    "title": "아이폰 14 프로 맥스",
    "price": 1200000,
    "category": "전자기기",
    "imageUrl": "/uploads/abc123.jpg",
    "status": "AVAILABLE"
  }
]
```

---

### 🔵 상품 상세 조회
**GET** `/api/products/{id}`

**Response:**
```json
{
  "productId": 1,
  "title": "아이폰 14 프로 맥스",
  "description": "거의 새 제품, 케이스 포함",
  "price": 1200000,
  "category": "전자기기",
  "imageUrl": "/uploads/abc123.jpg",
  "sellerNickname": "판매자닉네임",
  "status": "AVAILABLE"
}
```

---

### 🔵 상품 구매
**POST** `/api/products/{id}/buy`

**Headers:**
```
Authorization: Bearer JWT_TOKEN
```

**Response:**
```json
{
  "message": "구매 완료되었습니다.",
  "productId": 1
}
```

---

### 🔵 내가 등록한 상품 목록
**GET** `/api/products/mine`

**Headers:**
```
Authorization: Bearer JWT_TOKEN
```

**Response:**
```json
[
  {
    "productId": 1,
    "title": "아이폰 14 프로 맥스",
    "price": 1200000,
    "status": "SOLD"
  }
]
```

---

### 🔵 내가 구매한 상품 목록
**GET** `/api/products/purchased`

**Headers:**
```
Authorization: Bearer JWT_TOKEN
```

**Response:**
```json
[
  {
    "productId": 3,
    "title": "갤럭시 S24",
    "price": 900000,
    "sellerNickname": "판매자닉네임",
    "purchasedAt": "2024-01-15T10:30:00"
  }
]
```

---

### 🔵 파일 업로드
**POST** `/api/files/upload`

**Headers:**
```
Content-Type: multipart/form-data
```

**Request Body:**
```
file: (이미지 파일)
```

**Response:**
```json
{
  "imageUrl": "/uploads/uuid123.jpg",
  "message": "파일 업로드가 완료되었습니다."
}
```

---

## 📋 상품 상태 (ProductStatus)

| 상태 | 설명 |
|------|------|
| `AVAILABLE` | 판매 중 |
| `SOLD` | 판매 완료 |

## 🏗️ 데이터베이스 스키마 (네이버 클라우드 공유 DB)

**연결 정보:**
- Host: `223.130.162.28:30100`
- Database: `bookdb`
- Username: `root`
- 테이블 접두사: `carrot_` (팀별 구분용)

### carrot_products 테이블
```sql
CREATE TABLE carrot_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(19,2) NOT NULL,
    category VARCHAR(100),
    image_url VARCHAR(500),
    seller_id BIGINT NOT NULL,
    seller_nickname VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### carrot_purchases 테이블
```sql
CREATE TABLE carrot_purchases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    purchase_price DECIMAL(19,2) NOT NULL,
    purchased_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 🌐 환경 설정

### 로컬 개발 환경
```yaml
spring:
  datasource:
    url: jdbc:mysql://223.130.162.28:30100/bookdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
    username: root
    password: rootpassword
```

### 테스트 방법
```bash
# 애플리케이션 실행
./gradlew bootRun

# API 테스트
curl -X GET http://localhost:8082/api/products

# DB 직접 확인
mysql -h 223.130.162.28 -P 30100 -u root -p bookdb -e "SELECT * FROM carrot_products LIMIT 5;"
```
