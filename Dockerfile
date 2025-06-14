# Product Service Dockerfile
FROM openjdk:17-jdk-slim

# 메타데이터
LABEL maintainer="Mini Carrot Team"
LABEL service="product-service"
LABEL version="1.0.0"

# 작업 디렉토리 설정
WORKDIR /app

# 필요한 패키지 설치 (netcat for health check)
RUN apt-get update && apt-get install -y netcat-openbsd && rm -rf /var/lib/apt/lists/*

# 파일 업로드 디렉토리 생성
RUN mkdir -p /app/uploads

# JAR 파일 복사
COPY build/libs/mini-carrot-product-service-0.0.1-SNAPSHOT.jar app.jar

# 이미지 파일들을 uploads 폴더로 복사
COPY uploads/ /app/uploads/

# 포트 노출
EXPOSE 8082

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]