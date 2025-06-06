package com.minicarrot.product.repository;

import com.minicarrot.product.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    
    // 구매자별 구매 내역 조회 (userId 사용)
    List<Purchase> findByUserId(Long userId);
    
    // 특정 상품의 구매 내역 조회
    List<Purchase> findByProductId(Long productId);
    
    // 구매자별 구매 내역을 최신순으로 조회
    List<Purchase> findByUserIdOrderByPurchasedAtDesc(Long userId);
} 