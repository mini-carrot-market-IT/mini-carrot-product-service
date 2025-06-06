# 🔒 보안 가이드

## 📋 민감한 정보 관리

이 프로젝트에서는 다음과 같은 민감한 정보들이 사용됩니다:

### 🗃️ 데이터베이스 정보
- DB 호스트, 포트, 데이터베이스명
- DB 사용자명과 패스워드

### 🔑 JWT 설정
- JWT 비밀키 (최소 64자 이상)
- JWT 만료 시간

## 🚀 배포 전 보안 체크리스트

### ✅ 환경변수 설정
1. `env.example` 파일을 복사하여 `.env` 파일 생성
2. 모든 `YOUR_*` 값들을 실제 운영 값으로 교체
3. JWT_SECRET은 강력한 랜덤 문자열로 설정 (64자 이상)

### ✅ Kubernetes 배포
1. `k8s/product-service-simple.yaml`의 환경변수 템플릿 값들 교체
2. 민감한 정보는 Kubernetes Secret으로 관리 권장
3. `k8s/secrets-template.yml` 참고하여 Secret 리소스 생성

### ✅ Docker 이미지
- 이미지에 민감한 정보가 하드코딩되지 않도록 주의
- 환경변수를 통해 설정 주입

## 🚫 절대 커밋하지 말아야 할 파일들

```
# 실제 환경변수 파일
.env
.env.local
.env.prod

# 실제 운영 설정 파일
*-prod.yml
*-prod.yaml
*-secret.yml
*-secret.yaml

# 로그 파일
*.log
logs/

# 업로드된 파일들
uploads/
```

## 🔧 개발 환경 vs 운영 환경

### 개발 환경
- 환경변수를 통한 설정 주입
- 로컬 DB 또는 개발 DB 사용
- 디버그 로그 활성화

### 운영 환경
- Kubernetes Secret 사용 권장
- 운영 DB 연결
- 최소한의 로그만 출력
- SSL/TLS 적용

## 🛡️ 추가 보안 권장사항

1. **정기적인 비밀키 교체**
2. **DB 접근 권한 최소화**
3. **HTTPS 적용**
4. **방화벽 설정**
5. **모니터링 및 알람 설정** 