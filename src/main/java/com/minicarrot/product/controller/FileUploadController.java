package com.minicarrot.product.controller;

import com.minicarrot.product.dto.ApiResponse;
import com.minicarrot.product.service.FileService;
import com.minicarrot.product.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileService fileService;
    private final JwtService jwtService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        log.info("파일 업로드 요청 받음 - Content-Type: {}", 
                 org.springframework.web.context.request.RequestContextHolder
                 .currentRequestAttributes().toString());
        
        try {
            // Authorization 헤더 체크
            if (token == null || token.isEmpty()) {
                log.warn("Authorization 헤더가 누락됨");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("인증 토큰이 필요합니다."));
            }
            
            // JWT 토큰 검증
            if (!jwtService.validateToken(token)) {
                log.warn("유효하지 않은 JWT 토큰: {}", token);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("유효하지 않은 토큰입니다."));
            }
            
            // 파일 검증
            if (file == null) {
                log.warn("파일 파라미터가 null임");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("업로드할 파일이 없습니다. multipart/form-data 형식으로 'file' 필드를 포함해 주세요."));
            }
            
            if (file.isEmpty()) {
                log.warn("업로드된 파일이 비어있음");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("업로드할 파일을 선택해주세요."));
            }
            
            log.info("파일 업로드 시작 - 파일명: {}, 크기: {}", file.getOriginalFilename(), file.getSize());
            
            String fileUrl = fileService.uploadFile(file);
            
            Map<String, String> data = new HashMap<>();
            data.put("imageUrl", fileUrl);
            data.put("message", "파일 업로드가 완료되었습니다.");
            
            log.info("파일 업로드 성공 - URL: {}", fileUrl);
            
            return ResponseEntity.ok(ApiResponse.success("파일 업로드가 완료되었습니다.", data));
            
        } catch (MultipartException e) {
            log.error("Multipart 요청 처리 중 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("파일 업로드는 multipart/form-data 형식으로 요청해야 합니다."));
        } catch (Exception e) {
            log.error("파일 업로드 중 예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("파일 업로드 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
} 