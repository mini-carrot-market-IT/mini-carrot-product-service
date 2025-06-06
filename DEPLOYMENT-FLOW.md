# 🚀 Mini 당근마켓 네이버 클라우드 NKS 전체 배포 흐름

## 📋 **전체 구조 (3단계)**

```
1. user-service → 2. product-service → 3. frontend
     ↓                ↓                ↓
  MySQL 연결      MySQL 연결      API 연동
  JWT 인증        JWT 검증        UI 제공
```

---

## 🏗️ **네이버 클라우드 NKS 배포 구조**

```
┌─────────────────────────────────────────────────────────┐
│                네이버 클라우드 NKS 클러스터                    │
├─────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐      │
│  │  Frontend   │  │user-service │  │product-svc  │      │
│  │   (Nginx)   │  │(Spring Boot)│  │(Spring Boot)│      │
│  │   Port 80   │  │   Port 8080 │  │   Port 8082 │      │
│  └─────────────┘  └─────────────┘  └─────────────┘      │
│         │               │               │               │
│         └───────────────┼───────────────┘               │
│                         │                               │
│                  ┌─────────────┐                        │
│                  │    MySQL    │                        │
│                  │   Port 3306 │                        │
│                  └─────────────┘                        │
├─────────────────────────────────────────────────────────┤
│  공인 IP: 223.130.154.117 (LoadBalancer)                │
└─────────────────────────────────────────────────────────┘
```

---

## 🎯 **1단계: user-service 배포**

### **디렉토리 구조**
```
mini-carrot-user-service/
├── src/main/java/...
├── build.gradle
├── Dockerfile
├── k8s/
│   └── user-service-nks-deployment.yaml
└── deploy-nks.sh
```

### **배포 명령어**
```bash
cd mini-carrot-user-service

# 간단 배포
./deploy-nks.sh

# 또는 수동 배포
./gradlew build
docker build -t user-service:latest .
kubectl apply -f k8s/user-service-nks-deployment.yaml -n tuk-trainee12
```

### **서비스 역할**
- **API**: `/api/users/register`, `/api/users/login`, `/api/users/profile`
- **JWT 인증**: 로그인 시 토큰 발급
- **MySQL**: 사용자 정보 저장/조회

---

## 🎯 **2단계: product-service 배포 ✅ 완료**

### **디렉토리 구조**
```
mini-carrot-product-service/
├── src/main/java/...
├── build.gradle
├── Dockerfile
├── k8s/
│   └── product-service-nks-deployment.yaml
└── deploy-nks.sh                    ← 생성 완료!
```

### **배포 명령어**
```bash
cd mini-carrot-product-service

# 간단 배포
./deploy-nks.sh

# 또는 수동 배포
./gradlew build
docker build -t product-service:latest .
kubectl apply -f k8s/product-service-nks-deployment.yaml -n tuk-trainee12
```

### **서비스 역할**
- **API**: `/api/products` (CRUD), `/api/products/{id}/buy`
- **구매 처리**: 상품 상태 변경, 구매 내역 저장
- **MySQL**: 상품, 구매 정보 저장/조회
- **JWT 검증**: user-service와 토큰 호환

---

## 🎯 **3단계: frontend 배포**

### **디렉토리 구조**
```
mini-carrot-frontend/
├── src/...
├── package.json
├── Dockerfile
├── nginx.conf
├── k8s/
│   └── frontend-nks-deployment.yaml
└── deploy-nks.sh
```

### **배포 명령어**
```bash
cd mini-carrot-frontend

# 간단 배포
./deploy-nks.sh

# 또는 수동 배포
npm run build
docker build -t frontend:latest .
kubectl apply -f k8s/frontend-nks-deployment.yaml -n tuk-trainee12
```

### **서비스 역할**
- **웹 UI**: React/Next.js 기반 사용자 인터페이스
- **API 프록시**: `/api/users/*` → user-service, `/api/products/*` → product-service
- **정적 파일**: Nginx로 빌드된 파일 서빙

---

## 📦 **배포 순서**

### **1단계: MySQL 준비**
```bash
# MySQL 서비스가 이미 실행 중인지 확인
kubectl get svc mysql-service -n tuk-trainee12
```

### **2단계: Backend 서비스 배포**
```bash
# user-service 먼저 배포 (JWT 제공)
cd mini-carrot-user-service
./deploy-nks.sh

# product-service 배포 (JWT 소비)
cd mini-carrot-product-service
./deploy-nks.sh
```

### **3단계: Frontend 배포**
```bash
# 마지막에 frontend 배포 (LoadBalancer)
cd mini-carrot-frontend
./deploy-nks.sh
```

### **4단계: 외부 IP 확인**
```bash
kubectl get svc frontend-service -n tuk-trainee12
# EXTERNAL-IP: 223.130.154.117
```

---

## 🧪 **최종 시나리오 테스트**

### **브라우저 접속**
```
http://223.130.154.117
```

### **완전한 시나리오**
1. **회원가입**: 새 계정 생성
2. **로그인**: JWT 토큰 받기
3. **상품 등록**: 상품 정보 입력
4. **상품 목록**: 등록된 상품 확인
5. **상품 구매**: 다른 계정으로 구매
6. **마이페이지**: 거래 내역 확인

---

## 🎉 **서비스 간 통신 흐름**

### **회원가입/로그인**
```
브라우저 → Frontend(Nginx) → user-service → MySQL
                ↓
        JWT 토큰 반환 → 브라우저에 저장
```

### **상품 등록**
```
브라우저 → Frontend(Nginx) → product-service → MySQL
   ↑                              ↓
JWT 헤더 포함              user-service (토큰 검증)
```

### **상품 조회/구매**
```
브라우저 → Frontend(Nginx) → product-service → MySQL
                                ↓
                        상품 상태 업데이트 (SOLD)
                        구매 내역 저장
```

---

## 📊 **현재 상태**

| 서비스 | 상태 | 배포 스크립트 | 매니페스트 |
|--------|------|--------------|-----------|
| **user-service** | 🔄 준비 필요 | ❌ | ❌ |
| **product-service** | ✅ **완료** | ✅ `deploy-nks.sh` | ✅ `k8s/product-service-nks-deployment.yaml` |
| **frontend** | 🔄 준비 필요 | ❌ | ❌ |

---

## 🚀 **다음 할 일**

1. **user-service**: 동일한 패턴으로 NKS 배포 스크립트 생성
2. **frontend**: React 프로젝트에 Docker + NKS 설정 추가
3. **전체 테스트**: 3개 서비스 연동 확인

**🎯 이제 product-service는 완전히 준비되었습니다!**
**네이버 클라우드 NKS에 바로 배포할 수 있습니다!** 🎉 