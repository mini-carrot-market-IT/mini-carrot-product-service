# 🚀 Mini 당근마켓 Product Service 배포 가이드

## 📋 개요

본 문서는 **Product Service**의 프로덕션 배포를 위한 상세 가이드입니다.

## 🏗️ 서비스 정보

### 기본 정보
- **서비스명**: mini-carrot-product-service
- **포트**: 8082
- **프레임워크**: Spring Boot 3.2.0
- **Java 버전**: 17
- **빌드 도구**: Gradle 8.14

### 주요 기능
- 상품 CRUD 관리
- 파일 업로드 및 이미지 서빙
- JWT 기반 사용자 인증
- 상품 거래 및 구매 관리
- User Service와 연동

## 🔧 사전 요구사항

### 인프라 요구사항
- **Kubernetes 클러스터** (v1.20+)
- **MySQL 데이터베이스** (8.0+)
- **Persistent Volume** (파일 업로드용)
- **Load Balancer** (트래픽 분산)
- **Secret Management** (환경변수 보안)

### 연동 서비스
- **User Service**: JWT 토큰 호환성 필요
- **Frontend**: CORS 설정 필요
- **Database**: 기존 테이블과 호환 필요

## 📦 빌드 및 이미지 생성

### 1. 애플리케이션 빌드
```bash
./gradlew clean build

# 빌드 결과 확인
ls -la build/libs/
# mini-carrot-product-service-0.0.1-SNAPSHOT.jar (46MB)
```

### 2. Docker 이미지 빌드
```bash
# 이미지 빌드
docker build -t minicarrot/product-service:latest .

# 이미지 확인
docker images | grep product-service

# 컨테이너 레지스트리에 푸시
docker tag minicarrot/product-service:latest your-registry/minicarrot/product-service:v1.0.0
docker push your-registry/minicarrot/product-service:v1.0.0
```

## 🌍 환경변수 설정

### 필수 환경변수
| 변수명 | 설명 | 예시값 | 비고 |
|--------|------|--------|------|
| `DB_URL` | 데이터베이스 연결 URL | `jdbc:mysql://db:3306/minicarrot` | MySQL 8.0+ |
| `DB_USERNAME` | DB 사용자명 | `minicarrot_user` | |
| `DB_PASSWORD` | DB 비밀번호 | `strong_password` | Secret 관리 |
| `JWT_SECRET` | JWT 비밀키 | `minicarrotusersecret...` | User Service와 동일 |
| `JWT_EXPIRATION` | JWT 만료시간(ms) | `86400000` | 24시간 |

### 선택 환경변수
| 변수명 | 설명 | 기본값 | 운영권장값 |
|--------|------|--------|-----------|
| `JPA_DDL_AUTO` | JPA DDL 모드 | `update` | `validate` |
| `JPA_SHOW_SQL` | SQL 로그 출력 | `true` | `false` |
| `LOG_LEVEL` | 로그 레벨 | `DEBUG` | `INFO` |
| `SPRING_PROFILES_ACTIVE` | 활성 프로필 | `local` | `prod` |

## 🗄️ 데이터베이스 설정

### 기존 테이블 구조
```sql
-- 기존 네이버 클라우드 DB 테이블들
CREATE TABLE carrot_products (
    product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
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

CREATE TABLE carrot_purchases (
    purchase_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    purchased_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 연결 테스트
```bash
# 데이터베이스 연결 테스트
mysql -h your-db-host -P 3306 -u username -p minicarrot_db
```

## 🚀 Kubernetes 배포

### 1. 네임스페이스 생성 (선택사항)
```bash
kubectl create namespace minicarrot
```

### 2. Secret 생성
```bash
# 데이터베이스 Secret
kubectl create secret generic database-secret \
  --from-literal=url="jdbc:mysql://your-db:3306/minicarrot" \
  --from-literal=username="db_username" \
  --from-literal=password="db_password" \
  -n minicarrot

# JWT Secret  
kubectl create secret generic jwt-secret \
  --from-literal=secret="minicarrotusersecretkeyforjwttoken123456789012345678901234567890" \
  -n minicarrot
```

### 3. ConfigMap 적용
```bash
kubectl apply -f k8s/configmap.yml -n minicarrot
```

### 4. 애플리케이션 배포
```bash
kubectl apply -f k8s/product-service-deployment.yml -n minicarrot
```

### 5. 배포 상태 확인
```bash
# Pod 상태 확인
kubectl get pods -n minicarrot -l app=product-service

# 서비스 상태 확인
kubectl get svc -n minicarrot -l app=product-service

# 로그 확인
kubectl logs -f deployment/product-service -n minicarrot
```

## 🔍 헬스체크 및 모니터링

### 헬스체크 엔드포인트
| 엔드포인트 | 용도 | 응답 |
|------------|------|------|
| `/actuator/health` | 전체 상태 확인 | `{"status":"UP"}` |
| `/actuator/ready` | 준비 상태 확인 | `{"status":"UP"}` |
| `/api/products` | API 동작 확인 | 상품 목록 JSON |

### 모니터링 지표
```yaml
metrics:
  - name: http_requests_total
    description: 총 HTTP 요청 수
  - name: http_request_duration_seconds
    description: HTTP 요청 처리 시간
  - name: jvm_memory_used_bytes
    description: JVM 메모리 사용량
  - name: jwt_validation_total
    description: JWT 검증 횟수
```

## 🔒 보안 설정

### CORS 설정
```yaml
# 운영환경 CORS 설정
cors:
  allowed_origins:
    - "https://minicarrot.co.kr"
    - "https://www.minicarrot.co.kr"
    - "https://admin.minicarrot.co.kr"
  allowed_methods: ["GET", "POST", "PUT", "DELETE"]
  allowed_headers: ["*"]
```

### 네트워크 정책
```yaml
# Kubernetes NetworkPolicy 예시
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: product-service-netpol
spec:
  podSelector:
    matchLabels:
      app: product-service
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: api-gateway
    ports:
    - protocol: TCP
      port: 8082
```

## 📈 확장성 및 성능

### Auto Scaling 설정
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: product-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: product-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### 리소스 요구사항
```yaml
resources:
  requests:
    memory: "512Mi"    # 최소 필요 메모리
    cpu: "250m"        # 최소 필요 CPU
  limits:
    memory: "1Gi"      # 최대 메모리
    cpu: "500m"        # 최대 CPU
```

## 🛠️ 문제 해결

### 일반적인 문제들

#### 1. 데이터베이스 연결 실패
```bash
# 연결 테스트
kubectl exec -it deployment/product-service -- \
  java -cp app.jar org.springframework.boot.loader.JarLauncher \
  --spring.datasource.url=$DB_URL \
  --spring.datasource.username=$DB_USERNAME \
  --spring.datasource.password=$DB_PASSWORD
```

#### 2. JWT 토큰 검증 실패
- User Service와 JWT_SECRET 동일한지 확인
- 토큰 만료 시간 확인
- 클레임 구조 호환성 확인

#### 3. 파일 업로드 실패
- PVC 마운트 상태 확인
- 디스크 용량 확인
- 권한 설정 확인

### 로그 분석
```bash
# 특정 에러 로그 검색
kubectl logs deployment/product-service | grep ERROR

# JWT 관련 로그 확인
kubectl logs deployment/product-service | grep JWT

# 데이터베이스 관련 로그 확인
kubectl logs deployment/product-service | grep -i mysql
```

## 🔄 CI/CD 파이프라인

### GitLab CI/CD 예시
```yaml
stages:
  - build
  - test
  - docker
  - deploy

build:
  stage: build
  script:
    - ./gradlew clean build
  artifacts:
    paths:
      - build/libs/*.jar

docker:
  stage: docker
  script:
    - docker build -t $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA .
    - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA

deploy:
  stage: deploy
  script:
    - kubectl set image deployment/product-service product-service=$CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
```

## 📞 연락처 및 지원

### 개발팀 연락처
- **Product Service Team**: product-team@minicarrot.co.kr
- **Slack**: #product-service-team
- **문서**: [Confluence 링크]

### 긴급 연락처
- **On-Call**: +82-10-xxxx-xxxx
- **Incident Channel**: #incident-response

---

**배포 완료 후 체크리스트:**
- [ ] 애플리케이스 정상 시작 확인
- [ ] 헬스체크 엔드포인트 응답 확인
- [ ] 데이터베이스 연결 확인
- [ ] User Service와 JWT 연동 확인
- [ ] 파일 업로드 기능 확인
- [ ] 모니터링 지표 수집 확인
- [ ] 로그 수집 확인
- [ ] Auto Scaling 동작 확인

**🎉 Product Service 배포 완료!** 