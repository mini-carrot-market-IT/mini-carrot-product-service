package com.minicarrot.product.repository;

import com.minicarrot.product.entity.ProductViewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductViewLogRepository extends JpaRepository<ProductViewLog, Long> {
    
    @Query("SELECT COUNT(p) FROM ProductViewLog p WHERE p.productId = :productId")
    Long countByProductId(@Param("productId") Long productId);
    
    @Query("SELECT COUNT(p) FROM ProductViewLog p")
    Long countTotalViews();
    
    @Query("SELECT p.category, COUNT(p) FROM ProductViewLog p WHERE p.category IS NOT NULL GROUP BY p.category")
    List<Object[]> countByCategory();
    
    @Query("SELECT p.productId, COUNT(p) as viewCount FROM ProductViewLog p GROUP BY p.productId ORDER BY viewCount DESC")
    List<Object[]> findTopViewedProducts();
    
    @Query("SELECT COUNT(p) FROM ProductViewLog p WHERE p.viewedAt >= :since")
    Long countViewsSince(@Param("since") LocalDateTime since);
} 