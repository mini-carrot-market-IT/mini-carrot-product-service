package com.minicarrot.product.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpHeaders;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class JwtService {
    
    private final SecretKey secretKey;
    private final long expirationTime;
    private final WebClient webClient;
    private final String userServiceUrl;
    
    // 🚀 성능 최적화: 사용자 정보 캐시 (5분 TTL)
    private final Map<String, UserCacheEntry> userCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MINUTES = 5;
    
    private static class UserCacheEntry {
        final Long userId;
        final String nickname;
        final String email;
        final long timestamp;
        
        UserCacheEntry(Long userId, String nickname, String email) {
            this.userId = userId;
            this.nickname = nickname;
            this.email = email;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > TimeUnit.MINUTES.toMillis(CACHE_TTL_MINUTES);
        }
    }
    
    public JwtService(@Value("${jwt.secret}") String secret,
                     @Value("${jwt.expiration}") long expirationTime,
                     @Value("${user-service.url:http://user-service.tuk-trainee12.svc.cluster.local:8081}") String userServiceUrl,
                     WebClient.Builder webClientBuilder) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationTime = expirationTime;
        this.userServiceUrl = userServiceUrl;
        this.webClient = webClientBuilder.build();
    }
    
    public Long extractUserId(String token) {
        // 0차: 캐시에서 확인 (가장 빠름)
        String tokenKey = getTokenKey(token);
        UserCacheEntry cached = userCache.get(tokenKey);
        if (cached != null && !cached.isExpired() && cached.userId != null) {
            return cached.userId;
        }
        
        // 1차: 로컬 JWT에서 추출 (빠름)
        Long userIdFromLocal = extractUserIdLocally(token);
        if (userIdFromLocal != null) {
            // 캐시에 저장
            cacheUserInfo(tokenKey, userIdFromLocal, null, null);
            return userIdFromLocal;
        }
        
        // 2차: User Service에서 사용자 정보 조회 (fallback)
        return extractUserIdFromUserService(token);
    }

    /**
     * 🚀 초고속 사용자 ID 추출 (User Service 호출 생략)
     */
    public Long extractUserIdFast(String token) {
        try {
            // 캐시 우선 확인
            String tokenKey = getTokenKey(token);
            UserCacheEntry cached = userCache.get(tokenKey);
            if (cached != null && !cached.isExpired() && cached.userId != null) {
                return cached.userId;
            }
            
            // 로컬 JWT에서만 추출 (User Service 호출 생략으로 속도 향상)
            Long userIdFromLocal = extractUserIdLocally(token);
            if (userIdFromLocal != null) {
                // 캐시에 저장
                cacheUserInfo(tokenKey, userIdFromLocal, null, null);
                return userIdFromLocal;
            }
            
            // 실패 시 기본값 반환 (User Service 호출 안함)
            log.warn("빠른 사용자 ID 추출 실패 - 토큰에서 추출 불가");
            return null;
            
        } catch (Exception e) {
            log.error("빠른 사용자 ID 추출 중 오류: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 🚀 초고속 닉네임 추출 (캐시 우선, User Service 호출 생략)
     */
    public String extractNicknameFast(String token) {
        try {
            // 캐시 우선 확인
            String tokenKey = getTokenKey(token);
            UserCacheEntry cached = userCache.get(tokenKey);
            if (cached != null && !cached.isExpired() && cached.nickname != null) {
                return cached.nickname;
            }
            
            // 로컬 JWT에서만 추출 (User Service 호출 생략으로 속도 향상)
            String nicknameFromLocal = extractNicknameLocally(token);
            if (nicknameFromLocal != null && !nicknameFromLocal.equals("알 수 없는 사용자")) {
                // 캐시에 저장
                cacheUserInfo(tokenKey, null, nicknameFromLocal, null);
                return nicknameFromLocal;
            }
            
            // 실패 시 기본값 반환 (User Service 호출 안함)
            log.warn("빠른 닉네임 추출 실패 - 토큰에서 추출 불가");
            return null;
            
        } catch (Exception e) {
            log.error("빠른 닉네임 추출 중 오류: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * User Service에서 사용자 ID 조회
     */
    private Long extractUserIdFromUserService(String token) {
        try {
            String tokenWithoutBearer = token.startsWith("Bearer ") ? token.substring(7) : token;
            
            Map<String, Object> response = webClient.get()
                    .uri(userServiceUrl + "/api/auth/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithoutBearer)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null) {
                    Number userId = (Number) data.get("userId");
                    if (userId != null) {
                        log.debug("User Service에서 사용자 ID 조회 성공: {}", userId.longValue());
                        return userId.longValue();
                    }
                }
            }
            
            log.debug("User Service에서 사용자 ID 조회 실패: {}", response);
            return null;
            
        } catch (Exception e) {
            log.warn("User Service에서 사용자 정보 조회 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 로컬 JWT에서 사용자 ID 추출 (기존 방식)
     */
    private Long extractUserIdLocally(String token) {
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
        // 1차: 로컬 JWT에서 추출 (빠름)
        String nicknameFromLocal = extractNicknameLocally(token);
        if (nicknameFromLocal != null && !nicknameFromLocal.equals("알 수 없는 사용자")) {
            return nicknameFromLocal;
        }
        
        // 2차: User Service에서 닉네임 조회 (fallback)
        String nicknameFromService = extractNicknameFromUserService(token);
        return nicknameFromService != null ? nicknameFromService : nicknameFromLocal;
    }
    
    /**
     * User Service에서 닉네임 조회
     */
    private String extractNicknameFromUserService(String token) {
        try {
            String tokenWithoutBearer = token.startsWith("Bearer ") ? token.substring(7) : token;
            
            Map<String, Object> response = webClient.get()
                    .uri(userServiceUrl + "/api/auth/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithoutBearer)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null) {
                    String nickname = (String) data.get("nickname");
                    if (nickname != null) {
                        log.debug("User Service에서 닉네임 조회 성공: {}", nickname);
                        return nickname;
                    }
                }
            }
            
            log.debug("User Service에서 닉네임 조회 실패: {}", response);
            return null;
            
        } catch (Exception e) {
            log.warn("User Service에서 닉네임 조회 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 로컬 JWT에서 닉네임 추출 (기존 방식)
     */
    private String extractNicknameLocally(String token) {
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
        // 1차: 로컬 JWT에서 추출 (빠름)
        String emailFromLocal = extractEmailLocally(token);
        if (emailFromLocal != null) {
            return emailFromLocal;
        }
        
        // 2차: User Service에서 이메일 조회 (fallback)
        return extractEmailFromUserService(token);
    }
    
    /**
     * User Service에서 이메일 조회
     */
    private String extractEmailFromUserService(String token) {
        try {
            String tokenWithoutBearer = token.startsWith("Bearer ") ? token.substring(7) : token;
            
            Map<String, Object> response = webClient.get()
                    .uri(userServiceUrl + "/api/auth/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithoutBearer)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null) {
                    String email = (String) data.get("email");
                    if (email != null) {
                        log.debug("User Service에서 이메일 조회 성공: {}", email);
                        return email;
                    }
                }
            }
            
            log.debug("User Service에서 이메일 조회 실패: {}", response);
            return null;
            
        } catch (Exception e) {
            log.warn("User Service에서 이메일 조회 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 로컬 JWT에서 이메일 추출 (기존 방식)
     */
    private String extractEmailLocally(String token) {
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
        // 1차: User Service에 토큰 검증 요청
        if (validateTokenWithUserService(token)) {
            log.debug("User Service를 통한 JWT 토큰 검증 성공");
            return true;
        }
        
        // 2차: 로컬 JWT 검증 (fallback)
        log.debug("User Service 검증 실패, 로컬 JWT 검증 시도");
        return validateTokenLocally(token);
    }
    
    /**
     * User Service에 토큰 검증 요청
     */
    private boolean validateTokenWithUserService(String token) {
        try {
            String tokenWithoutBearer = token.startsWith("Bearer ") ? token.substring(7) : token;
            
            log.debug("User Service 토큰 검증 요청 - URL: {}", userServiceUrl);
            
            Map<String, Object> response = webClient.post()
                    .uri(userServiceUrl + "/api/auth/validate")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithoutBearer)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                log.debug("User Service 토큰 검증 성공");
                return true;
            }
            
            log.debug("User Service 토큰 검증 실패: {}", response);
            return false;
            
        } catch (Exception e) {
            log.warn("User Service 토큰 검증 중 오류 발생: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 로컬 JWT 토큰 검증 (기존 방식)
     */
    private boolean validateTokenLocally(String token) {
        try {
            String tokenWithoutBearer = token.startsWith("Bearer ") ? token.substring(7) : token;
            
            log.debug("JWT 토큰 검증 시작 - 토큰 길이: {}", tokenWithoutBearer.length());
            log.debug("사용 중인 JWT 비밀키 길이: {}", secretKey.getEncoded().length);
            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(tokenWithoutBearer)
                    .getBody();
            
            log.debug("JWT 토큰 검증 성공 - Subject: {}, UserId: {}", 
                     claims.getSubject(), claims.get("userId"));
            
            return true;
        } catch (ExpiredJwtException e) {
            log.error("JWT 토큰이 만료되었습니다: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.error("잘못된 형식의 JWT 토큰입니다: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.error("JWT 토큰 서명이 유효하지 않습니다: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰이 비어있거나 null입니다: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.error("JWT 토큰 검증 실패 (일반 오류): {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("JWT 토큰 검증 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
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
    
    // 🚀 캐시 관련 헬퍼 메서드들
    private String getTokenKey(String token) {
        String tokenWithoutBearer = token.startsWith("Bearer ") ? token.substring(7) : token;
        // 토큰의 해시값을 키로 사용 (보안상 전체 토큰을 키로 사용하지 않음)
        return String.valueOf(tokenWithoutBearer.hashCode());
    }
    
    private void cacheUserInfo(String tokenKey, Long userId, String nickname, String email) {
        try {
            UserCacheEntry entry = new UserCacheEntry(userId, nickname, email);
            userCache.put(tokenKey, entry);
            
            // 캐시 크기 제한 (메모리 관리)
            if (userCache.size() > 1000) {
                cleanExpiredCache();
            }
        } catch (Exception e) {
            log.warn("사용자 정보 캐시 저장 중 오류: {}", e.getMessage());
        }
    }
    
    private void cleanExpiredCache() {
        try {
            userCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
            log.debug("만료된 캐시 항목 정리 완료. 현재 캐시 크기: {}", userCache.size());
        } catch (Exception e) {
            log.warn("캐시 정리 중 오류: {}", e.getMessage());
        }
    }
} 