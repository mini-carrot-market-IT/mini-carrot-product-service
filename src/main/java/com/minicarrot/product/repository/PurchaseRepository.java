package com.minicarrot.product.repository;

import com.minicarrot.product.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    
    // 구매자별 구매 내역 조회 (userId 사용)
    List<Purchase> findByUserId(Long userId);
    
    // 특정 상품의 구매 내역 조회
    List<Purchase> findByProductId(Long productId);
    
    // 구매자별 구매 내역을 최신순으로 조회
    List<Purchase> findByUserIdOrderByPurchasedAtDesc(Long userId);
    
    // 판매자별 판매 내역 조회 (sellerId 사용)
    List<Purchase> findBySellerId(Long sellerId);
    
    // 사용자별 구매 통계를 위한 count 메서드들
    long countByUserId(Long userId);
    
    long countBySellerId(Long sellerId);
    
    // 🚀 성능 최적화: 사용자별 구매/판매 통계를 한 번의 쿼리로 조회
    @Query("SELECT COUNT(p), COALESCE(SUM(p.purchasePrice), 0) FROM Purchase p WHERE p.userId = :userId")
    Object[] getUserPurchaseStats(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(p), COALESCE(SUM(p.purchasePrice), 0) FROM Purchase p WHERE p.sellerId = :sellerId")
    Object[] getUserSalesStats(@Param("sellerId") Long sellerId);
    
    // 🚀 성능 최적화: 사용자의 구매/판매 통계를 한 번에 조회 (단순화)
    @Query("SELECT COUNT(p), COALESCE(SUM(p.purchasePrice), 0) FROM Purchase p WHERE p.userId = :userId")
    Object[] getUserPurchaseStatsSimple(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(p), COALESCE(SUM(p.purchasePrice), 0) FROM Purchase p WHERE p.sellerId = :userId")
    Object[] getUserSalesStatsSimple(@Param("userId") Long userId);
} 