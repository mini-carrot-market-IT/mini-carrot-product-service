# 🛠 Mini Carrot Product Service 설정 가이드

이 문서는 프로젝트를 처음 세팅하는 개발자를 위한 단계별 가이드입니다.

## 📋 체크리스트

- [ ] Java 17 설치
- [ ] MySQL 설치 및 실행
- [ ] 프로젝트 클론
- [ ] 데이터베이스 생성
- [ ] 설정 파일 생성
- [ ] 애플리케이션 실행

## 1. 🔧 사전 준비

### Java 17 설치

```bash
# macOS (Homebrew)
brew install openjdk@17

# Ubuntu
sudo apt install openjdk-17-jdk

# Windows
https://adoptium.net/ 에서 OpenJDK 17 다운로드
```

### MySQL 설치

```bash
# macOS (Homebrew)
brew install mysql
brew services start mysql

# Ubuntu
sudo apt install mysql-server
sudo systemctl start mysql

# Windows
https://dev.mysql.com/downloads/mysql/ 에서 다운로드
```

## 2. 📁 프로젝트 설정

### 프로젝트 클론

```bash
git clone https://github.com/YOUR_USERNAME/mini-carrot-product-service.git
cd mini-carrot-product-service
```

### 데이터베이스 생성

MySQL에 연결하여 데이터베이스를 생성합니다:

```bash
# MySQL 접속
mysql -u root -p

# 데이터베이스 생성
CREATE DATABASE mini_carrot_product CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 사용자 생성 (선택사항)
CREATE USER 'carrot_user'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON mini_carrot_product.* TO 'carrot_user'@'localhost';
FLUSH PRIVILEGES;

# 종료
EXIT;
```

## 3. ⚙️ 설정 파일 생성

### application-local.yml 생성

```bash
# 템플릿 복사
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

`src/main/resources/application-local.yml` 파일을 열어서 다음 정보를 수정:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mini_carrot_product?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
    username: root  # 또는 생성한 사용자명
    password: your_mysql_password  # 실제 MySQL 비밀번호
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

jwt:
  secret: minicarrotusersecretkeyforjwttoken123456789012345678901234567890  # User Service와 동일해야 함

logging:
  level:
    com.minicarrot.product: DEBUG
    org.springframework.web: DEBUG
```

### 환경변수 파일 생성 (선택사항)

```bash
# 템플릿 복사
cp env.example .env
```

`.env` 파일을 열어서 실제 값으로 수정:

```env
DB_URL=jdbc:mysql://localhost:3306/mini_carrot_product?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
JWT_SECRET=minicarrotusersecretkeyforjwttoken123456789012345678901234567890
```

## 4. 🚀 실행

### Gradle 실행

```bash
# 권한 설정 (Linux/macOS)
chmod +x gradlew

# 애플리케이션 실행
./gradlew bootRun
```

### 성공 확인

브라우저에서 다음 URL 접속:
- http://localhost:8082/api/products

정상적으로 실행되면 빈 배열 `[]` 또는 상품 목록이 표시됩니다.

## 5. 🧪 테스트

### API 테스트

```bash
# 상품 목록 조회
curl -X GET http://localhost:8082/api/products

# 더미데이터가 있다면 다음과 같은 응답을 받습니다:
[
  {
    "productId": 1,
    "title": "테스트 상품",
    "price": 10000,
    "category": "전자기기",
    "imageUrl": "/uploads/test.jpg",
    "status": "AVAILABLE"
  }
]
```

## 6. 🔍 문제 해결

### 자주 발생하는 오류와 해결책

#### 1. "Access denied for user" 오류

```
Cause: java.sql.SQLException: Access denied for user 'root'@'localhost'
```

**해결책:**
- MySQL 비밀번호 확인
- application-local.yml의 username/password 확인

#### 2. "Unknown database" 오류

```
Cause: java.sql.SQLException: Unknown database 'mini_carrot_product'
```

**해결책:**
- 데이터베이스가 생성되었는지 확인
- 데이터베이스명 오타 확인

#### 3. "Port 8082 already in use" 오류

```
Web server failed to start. Port 8082 was already in use.
```

**해결책:**
```bash
# 8082 포트 사용 프로세스 확인
lsof -i :8082

# 프로세스 종료
kill -9 [PID]

# 또는 다른 포트 사용 (application-local.yml)
server:
  port: 8083
```

#### 4. "JWT secret too short" 오류

**해결책:**
- JWT secret이 최소 64자 이상인지 확인
- User Service와 동일한 secret 사용 확인

## 7. 🎯 다음 단계

### User Service 연동

이 Product Service는 User Service와 함께 동작합니다.

1. User Service 프로젝트도 클론하여 실행
2. 동일한 JWT secret 사용 확인
3. 프론트엔드에서 로그인 후 JWT 토큰으로 API 호출

### 더미 데이터 추가

실제 상품 데이터로 테스트하려면:

```sql
-- MySQL에서 실행
USE mini_carrot_product;

-- 더미 상품 추가
INSERT INTO carrot_products (title, description, price, category, image_url, seller_id, seller_nickname, status) VALUES
('iPhone 14', '거의 새 제품', 800000, '전자기기', '/uploads/iphone.jpg', 1, '판매자1', 'AVAILABLE'),
('MacBook Air', 'M1 칩', 1200000, '전자기기', '/uploads/macbook.jpg', 1, '판매자1', 'AVAILABLE');
```

## 📞 도움이 필요하면

1. **README.md** 파일 참조
2. **docs/API.md** API 문서 확인
3. GitHub Issues에 질문 등록
4. 로그 파일 확인: `logs/` 디렉토리

---

이 가이드를 따라했는데도 문제가 발생한다면, 다음 정보와 함께 Issue를 생성해 주세요:

- 운영체제 (Windows/macOS/Linux)
- Java 버전 (`java -version`)
- MySQL 버전 (`mysql --version`)
- 전체 에러 로그 