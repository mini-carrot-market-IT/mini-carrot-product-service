#!/bin/bash

echo "🚀 Mini 당근마켓 Product Service 네이버 클라우드 배포 시작!"

# 색상 설정
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 1단계: 애플리케이션 빌드
echo -e "${BLUE}📦 1단계: 애플리케이션 빌드 중...${NC}"
if ./gradlew clean build -x test; then
    echo -e "${GREEN}✅ 빌드 성공!${NC}"
else
    echo -e "${RED}❌ 빌드 실패!${NC}"
    exit 1
fi

# 2단계: Docker 이미지 빌드
echo -e "${BLUE}🐳 2단계: Docker 이미지 빌드 중...${NC}"
if docker build -t ncloud-registry.kr/minicarrot/product-service:v1.0.0 .; then
    echo -e "${GREEN}✅ Docker 이미지 빌드 성공!${NC}"
else
    echo -e "${RED}❌ Docker 이미지 빌드 실패!${NC}"
    exit 1
fi

# 3단계: 이미지 확인
echo -e "${BLUE}🔍 3단계: 이미지 확인...${NC}"
docker images | grep ncloud-registry.kr/minicarrot/product-service

# 4단계: Kubernetes 배포
echo -e "${BLUE}☸️  4단계: Kubernetes 배포 중...${NC}"
if kubectl apply -f k8s/product-service-deployment.yml; then
    echo -e "${GREEN}✅ Kubernetes 배포 성공!${NC}"
else
    echo -e "${RED}❌ Kubernetes 배포 실패!${NC}"
    exit 1
fi

# 5단계: 배포 상태 확인
echo -e "${BLUE}📊 5단계: 배포 상태 확인 중...${NC}"
echo "Pod 상태:"
kubectl get pods -l app=product-service

echo -e "\n서비스 상태:"
kubectl get svc -l app=product-service

# 6단계: 외부 IP 확인 대기
echo -e "${YELLOW}⏳ LoadBalancer 외부 IP 할당 대기 중... (최대 5분)${NC}"
for i in {1..30}; do
    EXTERNAL_IP=$(kubectl get svc product-service -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null)
    if [[ -n "$EXTERNAL_IP" && "$EXTERNAL_IP" != "null" ]]; then
        echo -e "${GREEN}🎉 외부 IP 할당 완료: $EXTERNAL_IP${NC}"
        echo -e "${GREEN}🌐 웹 접속 URL: http://$EXTERNAL_IP/api/products${NC}"
        break
    fi
    echo "대기 중... ($i/30)"
    sleep 10
done

if [[ -z "$EXTERNAL_IP" || "$EXTERNAL_IP" == "null" ]]; then
    echo -e "${YELLOW}⚠️  외부 IP가 아직 할당되지 않았습니다. 수동으로 확인하세요:${NC}"
    echo "kubectl get svc product-service"
fi

# 7단계: 로그 확인
echo -e "${BLUE}📋 7단계: 애플리케이션 로그 확인...${NC}"
echo "최근 로그 (Ctrl+C로 중단):"
kubectl logs -f deployment/product-service --tail=10

echo -e "\n${GREEN}🎉 Mini 당근마켓 Product Service 배포 완료!${NC}" 