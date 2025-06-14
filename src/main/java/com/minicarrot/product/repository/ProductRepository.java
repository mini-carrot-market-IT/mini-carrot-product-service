package com.minicarrot.product.repository;

import com.minicarrot.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // 카테고리별 상품 조회
    List<Product> findByCategory(String category);
    
    // 판매자별 상품 조회
    List<Product> findBySellerId(Long sellerId);
    
    // 상품 상태별 조회
    List<Product> findByStatus(Product.ProductStatus status);
    
    // 카테고리와 상태로 조회
    List<Product> findByCategoryAndStatus(String category, Product.ProductStatus status);
    
    // 모든 상품을 최신순으로 조회
    List<Product> findAllByOrderByCreatedAtDesc();
    
    // 카테고리별 상품을 최신순으로 조회
    List<Product> findByCategoryOrderByCreatedAtDesc(String category);
    
    // 제목 검색 (대소문자 무시)
    List<Product> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String title);
    
    // 제목 검색 + 카테고리 필터
    List<Product> findByTitleContainingIgnoreCaseAndCategoryOrderByCreatedAtDesc(String title, String category);
    
    // 사용자별 상품 통계를 위한 count 메서드들
    long countBySellerId(Long sellerId);
    
    long countBySellerIdAndStatus(Long sellerId, Product.ProductStatus status);
    
    // 🚀 성능 최적화: 상태별 개수를 한 번의 쿼리로 조회
    @Query("SELECT p.status, COUNT(p) FROM Product p GROUP BY p.status")
    List<Object[]> countByStatusGrouped();
    
    // 🚀 성능 최적화: 카테고리별 개수를 한 번의 쿼리로 조회
    @Query("SELECT COALESCE(p.category, '기타'), COUNT(p) FROM Product p GROUP BY p.category")
    List<Object[]> countByCategoryGrouped();
    
    // 🚀 성능 최적화: 판매자별 상품 통계를 한 번의 쿼리로 조회
    @Query("SELECT p.status, COUNT(p) FROM Product p WHERE p.sellerId = :sellerId GROUP BY p.status")
    List<Object[]> countBySellerIdGroupedByStatus(@Param("sellerId") Long sellerId);
} 