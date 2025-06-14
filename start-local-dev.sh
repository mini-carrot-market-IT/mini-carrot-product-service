#!/bin/bash

echo "🥕 Mini Carrot 로컬 개발환경 시작! 🥕"
echo "=================================="

# Docker가 실행 중인지 확인
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker가 실행되지 않고 있습니다. Docker를 먼저 실행해주세요."
    exit 1
fi

echo "📦 Docker 컨테이너들을 시작합니다..."

# 기존 컨테이너 정리 (선택사항)
echo "🧹 기존 컨테이너 정리 중..."
docker-compose -f docker-compose-local.yml down

# 새로운 환경 시작
echo "🚀 새로운 환경을 시작합니다..."
docker-compose -f docker-compose-local.yml up -d

# 컨테이너 상태 확인
echo "⏳ 서비스가 준비될 때까지 기다립니다..."
sleep 15

# MySQL 연결 확인
echo "🗄️ MySQL 연결 확인 중..."
until docker exec mini-carrot-mysql mysqladmin ping -h localhost --silent; do
    echo "   MySQL 준비 중..."
    sleep 2
done

# RabbitMQ 연결 확인
echo "🐰 RabbitMQ 연결 확인 중..."
until docker exec mini-carrot-rabbitmq rabbitmq-diagnostics ping --silent; do
    echo "   RabbitMQ 준비 중..."
    sleep 2
done

echo ""
echo "✅ 로컬 개발환경이 준비되었습니다!"
echo ""
echo "📋 접속 정보:"
echo "   📊 phpMyAdmin: http://localhost:8081"
echo "   🗄️ MySQL: localhost:3306"
echo "     - Database: mini_carrot_product"
echo "     - Username: carrot_user"
echo "     - Password: carrot_pass"
echo ""
echo "   🐰 RabbitMQ 관리자: http://localhost:15672"
echo "     - Username: admin"
echo "     - Password: minicarrot123"
echo ""
echo "   📦 Redis: localhost:6379"
echo "     - Password: minicarrot123"
echo ""
echo "🚀 이제 Spring Boot 애플리케이션을 실행하세요:"
echo "   ./gradlew bootRun"
echo ""
echo "🔧 컨테이너 상태 확인:"
docker-compose -f docker-compose-local.yml ps

echo ""
echo "📝 유용한 명령어들:"
echo "   - 로그 확인: docker-compose -f docker-compose-local.yml logs -f [서비스명]"
echo "   - 컨테이너 정지: docker-compose -f docker-compose-local.yml down"
echo "   - 데이터 초기화: docker-compose -f docker-compose-local.yml down -v" 