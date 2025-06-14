package com.minicarrot.product.repository;

import com.minicarrot.product.entity.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {
    
    @Query("SELECT COUNT(s) FROM SearchLog s WHERE s.keyword = :keyword")
    Long countByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT COUNT(s) FROM SearchLog s")
    Long countTotalSearches();
    
    @Query("SELECT s.keyword, COUNT(s) as searchCount FROM SearchLog s GROUP BY s.keyword ORDER BY searchCount DESC")
    List<Object[]> findTopKeywords();
    
    @Query("SELECT s.category, COUNT(s) FROM SearchLog s WHERE s.category IS NOT NULL GROUP BY s.category")
    List<Object[]> countByCategory();
    
    @Query("SELECT COUNT(s) FROM SearchLog s WHERE s.searchedAt >= :since")
    Long countSearchesSince(@Param("since") LocalDateTime since);
} 