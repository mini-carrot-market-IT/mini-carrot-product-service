package com.minicarrot.product.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class FileService {

    private final String uploadDir = "uploads/";

    public String uploadFile(MultipartFile file) throws IOException {
        log.info("파일 업로드 시작: {}", file.getOriginalFilename());
        
        try {
            // 파일 기본 검증
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("업로드할 파일이 없습니다.");
            }

            // 업로드 디렉토리가 없으면 생성
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                log.info("업로드 디렉토리 생성: {}", directory.getAbsolutePath());
                boolean created = directory.mkdirs();
                if (!created) {
                    throw new IOException("업로드 디렉토리 생성에 실패했습니다: " + directory.getAbsolutePath());
                }
            }

            // 디렉토리 권한 확인
            if (!directory.canWrite()) {
                throw new IOException("업로드 디렉토리에 쓰기 권한이 없습니다: " + directory.getAbsolutePath());
            }

            // 파일명 검증 및 확장자 추출
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                originalFilename = "unknown";
            }
            
            String extension = "";
            if (originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 고유한 파일명 생성
            String uniqueFilename = UUID.randomUUID().toString() + extension;
            
            // 파일 저장 경로
            Path filePath = Paths.get(uploadDir + uniqueFilename);
            log.info("파일 저장 경로: {}", filePath.toAbsolutePath());
            
            // 파일 저장
            Files.copy(file.getInputStream(), filePath);
            
            log.info("파일 업로드 성공: {} -> {}", originalFilename, uniqueFilename);
            
            // 파일 URL 반환
            return "/uploads/" + uniqueFilename;
            
        } catch (Exception e) {
            log.error("파일 업로드 중 오류 발생", e);
            throw new IOException("파일 업로드에 실패했습니다: " + e.getMessage(), e);
        }
    }

    public boolean deleteFile(String filename) {
        try {
            if (filename == null || filename.isEmpty()) {
                log.warn("삭제할 파일명이 비어있습니다.");
                return false;
            }
            
            Path filePath = Paths.get(uploadDir + filename);
            boolean deleted = Files.deleteIfExists(filePath);
            
            if (deleted) {
                log.info("파일 삭제 성공: {}", filename);
            } else {
                log.warn("삭제할 파일이 존재하지 않습니다: {}", filename);
            }
            
            return deleted;
        } catch (IOException e) {
            log.error("파일 삭제 중 오류 발생: {}", filename, e);
            return false;
        }
    }
} 