# 🌐 Mini 당근마켓 Product Service 외부 접근 가이드

## 📋 현재 상황
- **Product Service**: `localhost:8083`에서 정상 실행 중
- **DB 연결**: 223.130.162.28:30100/bookdb 성공
- **API 상태**: 모든 엔드포인트 정상 작동

## 🚀 친구가 외부에서 접근하는 방법들

### **방법 1: 실제 서버 배포 (권장)**

#### **1-1. 네이버 클라우드 서버에 배포**
```bash
# 서버에서 Docker 실행
docker run -d --name product-service \
  -p 8083:8082 \
  -e JWT_SECRET="minicarrotusersecretkeyforjwttoken123456789012345678901234567890" \
  -e DB_URL="jdbc:mysql://223.130.162.28:30100/bookdb" \
  -e DB_USERNAME="root" \
  -e DB_PASSWORD="rootpassword" \
  minicarrot/product-service:latest

# 접근 URL: http://서버IP:8083/api/products
```

#### **1-2. 클라우드 인스턴스 생성**
- **네이버 클라우드**: VPC Server 생성 후 배포
- **AWS EC2**: 인스턴스 생성 후 Docker 실행
- **GCP Compute Engine**: VM 생성 후 서비스 실행

### **방법 2: 터널링 서비스 사용**

#### **2-1. ngrok 사용**
```bash
# 터미널 1: Product Service 실행
docker run -d --name product-service -p 8083:8082 [환경변수들...] minicarrot/product-service:latest

# 터미널 2: ngrok 터널 생성
ngrok http 8083

# 결과: https://abc123.ngrok.io -> localhost:8083
```

#### **2-2. LocalTunnel 사용**
```bash
npm install -g localtunnel
lt --port 8083 --subdomain minicarrot-product

# 결과: https://minicarrot-product.loca.lt -> localhost:8083
```

### **방법 3: 포트 포워딩 (같은 네트워크)**

#### **3-1. 라우터 설정**
- 라우터 관리 페이지에서 8083 포트 포워딩 설정
- 공인 IP:8083 -> 로컬 머신:8083

#### **3-2. 방화벽 설정**
```bash
# macOS 방화벽 해제 (임시)
sudo pfctl -d

# 특정 포트만 허용
sudo pfctl -e -f /etc/pf.conf
```

## 🔧 **현재 추천 방법**

### **✅ 즉시 사용 가능한 방법**

#### **Option A: ngrok 사용 (무료, 즉시 가능)**
```bash
# 1단계: ngrok 설치 (이미 완료)
brew install ngrok

# 2단계: 터널 생성
ngrok http 8083

# 3단계: 생성된 URL을 친구에게 전달
# 예시: https://1234-abcd-efgh.ngrok.io
```

#### **Option B: 클라우드 서버 배포 (안정적, 영구적)**
```bash
# 서버에서 실행할 명령어
docker run -d --name product-service \
  -p 80:8082 \
  -e JWT_SECRET="minicarrotusersecretkeyforjwttoken123456789012345678901234567890" \
  -e DB_URL="jdbc:mysql://223.130.162.28:30100/bookdb" \
  -e DB_USERNAME="root" \
  -e DB_PASSWORD="rootpassword" \
  minicarrot/product-service:latest

# 접근 URL: http://서버IP/api/products
```

## 📱 **친구의 Frontend와 연동**

### **Frontend 설정 수정**
친구의 Frontend(223.130.154.117)에서 API 호출 시:

```javascript
// Before (로컬)
const API_BASE_URL = 'http://localhost:8083';

// After (외부 접근)
const API_BASE_URL = 'https://your-ngrok-url.ngrok.io';
// 또는
const API_BASE_URL = 'http://서버IP:8083';
```

### **CORS 설정 확인**
현재 Product Service는 모든 Origin에서 접근 가능하도록 설정되어 있습니다:
```yaml
cors:
  allowed-origins: "*"
  allowed-methods: "GET,POST,PUT,DELETE"
  allowed-headers: "*"
```

## 🧪 **테스트 방법**

### **1. API 테스트**
```bash
# 상품 목록 조회
curl https://your-url/api/products

# 상품 상세 조회  
curl https://your-url/api/products/1

# JWT 인증 테스트
curl -H "Authorization: Bearer [JWT토큰]" https://your-url/api/products/mine
```

### **2. 브라우저 테스트**
- **상품 목록**: `https://your-url/api/products`
- **상품 상세**: `https://your-url/api/products/1`

## 🚨 **주의사항**

### **보안**
- **개발용**: ngrok, localtunnel 사용 가능
- **운영용**: 실제 서버 배포 + HTTPS 필수
- **JWT Secret**: 운영환경에서는 더 강력한 키 사용

### **성능**
- **ngrok**: 무료 플랜은 연결 제한 있음
- **터널링**: 네트워크 지연 발생 가능
- **서버 배포**: 최고 성능, 안정성

## 📞 **지원**

### **현재 서비스 상태**
- ✅ **Product Service**: 정상 실행 중
- ✅ **Database**: 연결 성공
- ✅ **API**: 모든 엔드포인트 작동
- ✅ **Docker**: 외부 접근 설정 완료

### **다음 단계**
1. **ngrok으로 즉시 테스트** (빠른 검증)
2. **클라우드 서버 배포** (안정적 운영)
3. **Frontend 연동 확인** (전체 시나리오 테스트)

---

**🎉 친구가 외부에서 우리 Product Service에 접근할 준비가 완료되었습니다!** 