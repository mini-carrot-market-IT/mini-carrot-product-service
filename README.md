# Mini Carrot Product Service 🥕

중고거래 플랫폼 "미니 당근마켓"의 상품 관리 서비스입니다.

## 📋 기능

- ✅ 상품 등록/조회/수정/삭제 (CRUD)
- ✅ 파일 업로드 (이미지)
- ✅ 상품 구매 기능
- ✅ 카테고리별 상품 필터링
- ✅ JWT 인증 기반 권한 관리
- ✅ 마이페이지 (내 상품, 구매 내역)

## 🛠 기술 스택

- **Backend**: Spring Boot 3.2.0, Java 17
- **Database**: MySQL 8.0
- **ORM**: Spring Data JPA
- **Authentication**: JWT
- **Build**: Gradle
- **Container**: Docker
- **Deploy**: Kubernetes

## 🚀 로컬 개발 환경 설정

### 1. 사전 요구사항

- Java 17 이상
- MySQL 8.0
- Docker (선택사항)

### 2. 데이터베이스 설정

```sql
-- MySQL에서 데이터베이스 생성
CREATE DATABASE mini_carrot_product;

-- 사용자 생성 및 권한 부여 (선택사항)
CREATE USER 'carrot_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON mini_carrot_product.* TO 'carrot_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. 로컬 설정 파일 생성

```bash
# application-local.yml 파일 생성
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml

# 실제 DB 정보로 수정
vi src/main/resources/application-local.yml
```

### 4. 환경변수 설정 (선택사항)

```bash
# .env 파일 생성
cp env.example .env

# 실제 값으로 수정
vi .env
```

### 5. 애플리케이션 실행

```bash
# Gradle로 실행
./gradlew bootRun

# 또는 JAR 빌드 후 실행
./gradlew build
java -jar build/libs/mini-carrot-product-service-0.0.1-SNAPSHOT.jar
```

서버가 시작되면 `http://localhost:8082`에서 접근 가능합니다.

## 🐳 Docker로 실행

### 1. Docker 이미지 빌드

```bash
# 애플리케이션 빌드
./gradlew build

# Docker 이미지 빌드
docker build -t mini-carrot-product-service .
```

### 2. Docker Compose 실행

```yaml
# docker-compose.yml 예시
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: mini_carrot_product
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  product-service:
    image: mini-carrot-product-service
    ports:
      - "8082:8082"
    environment:
      DB_URL: jdbc:mysql://mysql:3306/mini_carrot_product
      DB_USERNAME: root
      DB_PASSWORD: rootpassword
      JWT_SECRET: your_jwt_secret_key
    depends_on:
      - mysql

volumes:
  mysql_data:
```

```bash
docker-compose up -d
```

## ☸️ Kubernetes 배포

### 1. 설정 파일 준비

```bash
# Kubernetes 설정 파일 생성
cp k8s/product-service.yaml.example k8s/product-service.yaml

# 실제 값으로 수정
vi k8s/product-service.yaml
```

### 2. 배포

```bash
# 네임스페이스 생성
kubectl create namespace your-namespace

# 배포
kubectl apply -f k8s/product-service.yaml
```

## 📡 API 문서

### 주요 엔드포인트

- **GET** `/api/products` - 상품 목록 조회
- **GET** `/api/products/{id}` - 상품 상세 조회
- **POST** `/api/products` - 상품 등록 (JWT 필요)
- **PUT** `/api/products/{id}` - 상품 수정 (JWT 필요)
- **DELETE** `/api/products/{id}` - 상품 삭제 (JWT 필요)
- **POST** `/api/products/{id}/buy` - 상품 구매 (JWT 필요)
- **GET** `/api/products/mine` - 내 상품 목록 (JWT 필요)
- **GET** `/api/products/purchased` - 구매 내역 (JWT 필요)
- **POST** `/api/files/upload` - 파일 업로드 (JWT 필요)

자세한 API 문서는 [docs/API.md](docs/API.md)를 참조하세요.

## 🗄 데이터베이스 스키마

### carrot_products (상품)
- `product_id` - 상품 ID (PK)
- `title` - 상품명
- `description` - 상품 설명
- `price` - 가격
- `category` - 카테고리
- `image_url` - 이미지 URL
- `seller_id` - 판매자 ID
- `seller_nickname` - 판매자 닉네임
- `status` - 상품 상태 (AVAILABLE/SOLD)
- `created_at` - 생성일시
- `updated_at` - 수정일시

### carrot_purchases (구매)
- `purchase_id` - 구매 ID (PK)
- `user_id` - 구매자 ID
- `product_id` - 상품 ID (FK)
- `buyer_id` - 구매자 ID
- `seller_id` - 판매자 ID
- `purchase_price` - 구매 가격
- `purchase_status` - 구매 상태
- `purchased_at` - 구매일시

### carrot_users (사용자)
- `user_id` - 사용자 ID (PK)
- `email` - 이메일
- `nickname` - 닉네임
- `password` - 비밀번호 (암호화)
- `created_at` - 생성일시
- `updated_at` - 수정일시

## 🔗 관련 서비스

이 프로젝트는 마이크로서비스 아키텍처의 일부입니다:

- **User Service**: 사용자 인증/관리 서비스
- **Frontend**: React 기반 웹 애플리케이션

## 🤝 기여 방법

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

## 📞 문의

프로젝트에 대한 문의사항이 있으시면 Issue를 생성해 주세요.

## 🔧 트러블슈팅

### 자주 발생하는 문제들

1. **DB 연결 실패**
   - MySQL 서버가 실행 중인지 확인
   - 데이터베이스와 사용자 권한 확인
   - application-local.yml의 DB 설정 확인

2. **JWT 토큰 오류**
   - User Service와 동일한 JWT secret 사용 확인
   - 토큰 만료 시간 확인

3. **파일 업로드 실패**
   - uploads 디렉토리 권한 확인
   - 파일 크기 제한 확인 (기본 10MB)

4. **이미지가 안 보임**
   - uploads 폴더에 이미지 파일 존재 확인
   - static 리소스 경로 설정 확인