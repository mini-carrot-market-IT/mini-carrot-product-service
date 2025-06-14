package com.minicarrot.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "keyword", nullable = false)
    private String keyword;
    
    @Column(name = "category")
    private String category;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "result_count")
    private Integer resultCount;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "searched_at", nullable = false)
    private LocalDateTime searchedAt;
    
    @PrePersist
    protected void onCreate() {
        searchedAt = LocalDateTime.now();
    }
} 