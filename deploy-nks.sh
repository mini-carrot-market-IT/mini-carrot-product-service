#!/bin/bash

echo "🚀 Product Service → 네이버 클라우드 NKS 배포!"

# 색상 설정
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# 1단계: 빌드
echo -e "${BLUE}📦 1단계: JAR 빌드 중...${NC}"
if ./gradlew clean build -x test; then
    echo -e "${GREEN}✅ 빌드 완료!${NC}"
else
    echo -e "${RED}❌ 빌드 실패!${NC}"
    exit 1
fi

# 2단계: Docker 이미지 빌드
echo -e "${BLUE}🐳 2단계: Docker 이미지 빌드 중...${NC}"
if docker build -t product-service:latest .; then
    echo -e "${GREEN}✅ Docker 이미지 완료!${NC}"
else
    echo -e "${RED}❌ Docker 빌드 실패!${NC}"
    exit 1
fi

# 3단계: NKS 배포
echo -e "${BLUE}☸️  3단계: NKS 배포 중...${NC}"
if kubectl apply -f k8s/product-service-nks-deployment.yaml; then
    echo -e "${GREEN}✅ NKS 배포 완료!${NC}"
else
    echo -e "${RED}❌ NKS 배포 실패!${NC}"
    exit 1
fi

# 4단계: 상태 확인
echo -e "${BLUE}📊 4단계: 배포 상태 확인...${NC}"
echo "Pod 상태:"
kubectl get pods -n tuk-trainee12 -l app=product-service

echo -e "\nService 상태:"
kubectl get svc -n tuk-trainee12 -l app=product-service

echo -e "\n${GREEN}🎉 Product Service NKS 배포 완료!${NC}"
echo -e "${YELLOW}📱 Frontend에서 이제 product-service:80으로 호출 가능합니다!${NC}" 