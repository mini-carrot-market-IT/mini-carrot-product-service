package com.minicarrot.product.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class JwtService {
    
    private final SecretKey secretKey;
    private final long expirationTime;
    
    public JwtService(@Value("${jwt.secret}") String secret,
                     @Value("${jwt.expiration}") long expirationTime) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationTime = expirationTime;
    }
    
    public Long extractUserId(String token) {
        try {
            String tokenWithoutBearer = token.startsWith("Bearer ") ? token.substring(7) : token;
            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(tokenWithoutBearer)
                    .getBody();
            
            // User Service에서 userId는 Number 타입으로 저장됨
            Number userIdNumber = claims.get("userId", Number.class);
            return userIdNumber != null ? userIdNumber.longValue() : null;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT 토큰에서 userId 추출 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("유효하지 않은 토큰입니다.");
        }
    }
    
    public String extractNickname(String token) {
        try {
            String tokenWithoutBearer = token.startsWith("Bearer ") ? token.substring(7) : token;
            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(tokenWithoutBearer)
                    .getBody();
            
            String nickname = claims.get("nickname", String.class);
            return nickname != null ? nickname : "알 수 없는 사용자";
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT 토큰에서 nickname 추출 중 오류 발생: {}", e.getMessage());
            return "알 수 없는 사용자";
        }
    }
    
    public String extractEmail(String token) {
        try {
            String tokenWithoutBearer = token.startsWith("Bearer ") ? token.substring(7) : token;
            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(tokenWithoutBearer)
                    .getBody();
            
            return claims.getSubject(); // sub 클레임에서 이메일 추출
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT 토큰에서 email 추출 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }
    
    public boolean validateToken(String token) {
        try {
            String tokenWithoutBearer = token.startsWith("Bearer ") ? token.substring(7) : token;
            
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(tokenWithoutBearer);
            
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }
    
    // 디버깅용 메서드
    public void printTokenClaims(String token) {
        try {
            String tokenWithoutBearer = token.startsWith("Bearer ") ? token.substring(7) : token;
            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(tokenWithoutBearer)
                    .getBody();
            
            log.info("JWT 토큰 클레임 정보:");
            log.info("- Subject (email): {}", claims.getSubject());
            log.info("- userId: {}", claims.get("userId"));
            log.info("- nickname: {}", claims.get("nickname"));
            log.info("- iat: {}", claims.getIssuedAt());
            log.info("- exp: {}", claims.getExpiration());
        } catch (Exception e) {
            log.error("토큰 클레임 출력 중 오류: {}", e.getMessage());
        }
    }
} 