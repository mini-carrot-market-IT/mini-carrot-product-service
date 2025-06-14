package com.minicarrot.product.service;

import com.minicarrot.product.dto.*;
import com.minicarrot.product.dto.event.ProductEventDto;
import com.minicarrot.product.dto.event.AnalyticsEventDto;
import com.minicarrot.product.entity.Product;
import com.minicarrot.product.entity.Purchase;
import com.minicarrot.product.repository.ProductRepository;
import com.minicarrot.product.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final JwtService jwtService;
    private final EventPublisherService eventPublisherService;
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    // SSE 연결 관리를 위한 맵 추가
    private final Map<String, SseEmitter> productStreamEmitters = new ConcurrentHashMap<>();

    @Transactional
    public ProductCreateResponse createProduct(String token, ProductRequest request) {
        // 🚀 빠른 사용자 정보 추출 (캐시 우선)
        Long sellerId = jwtService.extractUserIdFast(token);
        String sellerNickname = null;
        
        if (sellerId == null) {
            // 폴백: 기존 방식으로 추출
            sellerId = jwtService.extractUserId(token);
            sellerNickname = jwtService.extractNickname(token);
        } else {
            // 빠른 방식으로 닉네임도 추출
            sellerNickname = jwtService.extractNicknameFast(token);
            if (sellerNickname == null) {
                sellerNickname = jwtService.extractNickname(token);
            }
        }
        
        // 2. 상품 엔티티 생성 및 저장 (핵심 로직만 동기 처리)
        Product product = new Product(
            request.getTitle(),
            request.getDescription(),
            request.getPrice(),
            request.getCategory(),
            request.getImageUrl(),
            sellerId,
            sellerNickname
        );
        
        Product savedProduct = productRepository.save(product);
        
        // 3. 비동기로 이벤트 발행 및 브로드캐스트 처리 (스프링 비동기 사용)
        processProductCreatedAsync(savedProduct);
        
        return new ProductCreateResponse(savedProduct.getId(), savedProduct.getStatus().toString());
    }

    /**
     * 상품 등록 후 비동기 처리 (스프링 비동기 + 성능 최적화)
     */
    @Async("productAsyncExecutor")
    public void processProductCreatedAsync(Product savedProduct) {
        try {
            // 🚀 상품 등록 이벤트 발행 (비동기)
            ProductEventDto productEvent = ProductEventDto.builder()
                    .productId(savedProduct.getId())
                    .title(savedProduct.getTitle())
                    .description(savedProduct.getDescription())
                    .price(savedProduct.getPrice().intValue())
                    .category(savedProduct.getCategory())
                    .imageUrl(savedProduct.getImageUrl())
                    .sellerId(savedProduct.getSellerId())
                    .sellerNickname(savedProduct.getSellerNickname())
                    .status(savedProduct.getStatus().toString())
                    .build();
            
            // 이벤트 발행도 비동기로 처리
            eventPublisherService.publishProductCreatedAsync(productEvent);
            
            // 실시간 스트림에 새 상품 즉시 반영 (비동기)
            broadcastNewProduct(savedProduct);
            
            log.debug("상품 등록 후처리 완료: {}", savedProduct.getId());
            
        } catch (Exception e) {
            log.error("상품 등록 후처리 중 오류 발생 (상품 ID: {}): {}", savedProduct.getId(), e.getMessage());
            // 후처리 실패해도 상품 등록은 성공으로 처리
        }
    }

    /**
     * 상품 목록 조회 (성능 최적화 버전 - 판매 중인 상품만, 조회수 포함)
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts(String category) {
        List<Product> products;
        
        if (category != null && !category.isEmpty()) {
            // 🚀 성능 최적화: 판매 중인 상품만 조회
            products = productRepository.findAvailableProductsByCategoryOrderByCreatedAtDesc(category);
        } else {
            // 🚀 성능 최적화: 판매 중인 상품만 조회
            products = productRepository.findAvailableProductsOrderByCreatedAtDesc();
        }
        
        // 🚀 성능 최적화: 스트림 처리 최적화 + 조회수 포함
        return products.parallelStream()
            .map(product -> new ProductResponse(
                product.getId(),
                product.getTitle(),
                product.getPrice(),
                product.getCategory(),
                product.getImageUrl(),
                product.getStatus().toString(),
                product.getViewCount()
            ))
            .collect(Collectors.toList());
    }

    /**
     * 상품 목록 조회 (페이지네이션 지원 - 판매 중인 상품만, 조회수 포함)
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsWithPagination(String category, int page, int size) {
        List<Product> products;
        
        if (category != null && !category.isEmpty()) {
            // 🚀 성능 최적화: 판매 중인 상품만 조회
            products = productRepository.findAvailableProductsByCategoryOrderByCreatedAtDesc(category);
        } else {
            // 🚀 성능 최적화: 판매 중인 상품만 조회
            products = productRepository.findAvailableProductsOrderByCreatedAtDesc();
        }
        
        // 페이지네이션 적용
        int start = page * size;
        int end = Math.min(start + size, products.size());
        
        if (start >= products.size()) {
            return new ArrayList<>();
        }
        
        return products.subList(start, end)
            .parallelStream()
            .map(product -> new ProductResponse(
                product.getId(),
                product.getTitle(),
                product.getPrice(),
                product.getCategory(),
                product.getImageUrl(),
                product.getStatus().toString(),
                product.getViewCount()
            ))
            .collect(Collectors.toList());
    }

    public List<ProductResponse> searchProducts(String query, String category) {
        List<Product> products;
        
        if (category != null && !category.isEmpty()) {
            products = productRepository.findByTitleContainingIgnoreCaseAndCategoryOrderByCreatedAtDesc(query, category);
        } else {
            products = productRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(query);
        }
        
        // 🔍 검색 분석 이벤트 발행
        AnalyticsEventDto searchAnalytics = AnalyticsEventDto.builder()
                .eventType("SEARCH")
                .searchKeyword(query)
                .searchCategory(category)
                .searchResultCount(products.size())
                .build();
        
        eventPublisherService.publishSearchAnalytics(searchAnalytics);
        
        return products.stream()
            .map(product -> new ProductResponse(
                product.getId(),
                product.getTitle(),
                product.getPrice(),
                product.getCategory(),
                product.getImageUrl(),
                product.getStatus().toString(),
                product.getViewCount()
            ))
            .collect(Collectors.toList());
    }

    @Transactional
    public ProductDetailResponse getProductDetail(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));
        
        // 🔢 조회수 증가 (간단하게!)
        product.incrementViewCount();
        productRepository.save(product);
        
        return new ProductDetailResponse(
            product.getId(),
            product.getTitle(),
            product.getDescription(),
            product.getPrice(),
            product.getCategory(),
            product.getImageUrl(),
            product.getSellerNickname(),
            product.getStatus().toString(),
            product.getViewCount()
        );
    }

    public ProductDetailResponse getProductForEdit(Long productId, String token) {
        Long userId = jwtService.extractUserId(token);
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));
        
        // 권한 검증: 자신의 상품인지 확인
        if (!product.isOwnedBy(userId)) {
            throw new RuntimeException("해당 상품을 조회할 권한이 없습니다.");
        }
        
        return new ProductDetailResponse(
            product.getId(),
            product.getTitle(),
            product.getDescription(),
            product.getPrice(),
            product.getCategory(),
            product.getImageUrl(),
            product.getSellerNickname(),
            product.getStatus().toString(),
            product.getViewCount()
        );
    }

    @Transactional
    public PurchaseResponse buyProduct(Long productId, String token) {
        Long buyerId = jwtService.extractUserId(token);
        String buyerNickname = jwtService.extractNickname(token);
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));
        
        // 비즈니스 메서드 사용
        if (product.isSold()) {
            throw new RuntimeException("이미 판매된 상품입니다.");
        }
        
        if (product.isOwnedBy(buyerId)) {
            throw new RuntimeException("자신의 상품은 구매할 수 없습니다.");
        }
        
        // 구매 기록 생성 (user_id는 구매자, seller_id는 상품의 판매자, purchase_price는 상품 가격)
        Purchase purchase = new Purchase(productId, buyerId, product.getSellerId(), product.getPrice());
        purchaseRepository.save(purchase);
        
        // 비즈니스 메서드로 상태 변경
        product.markAsSold();
        Product savedProduct = productRepository.save(product);
        
        // 비동기로 구매 이벤트 발행 (스프링 비동기 사용)
        processPurchaseEventAsync(savedProduct, buyerId, buyerNickname);
        
        return new PurchaseResponse("구매 완료되었습니다.", productId);
    }

    /**
     * 상품 구매 후 비동기 처리 (스프링 비동기 + 성능 최적화)
     */
    @Async("productAsyncExecutor")
    public void processPurchaseEventAsync(Product product, Long buyerId, String buyerNickname) {
        try {
            // 🛒 상품 구매 이벤트 발행 (비동기)
            ProductEventDto purchaseEvent = ProductEventDto.builder()
                    .productId(product.getId())
                    .title(product.getTitle())
                    .description(product.getDescription())
                    .price(product.getPrice().intValue())
                    .category(product.getCategory())
                    .imageUrl(product.getImageUrl())
                    .sellerId(product.getSellerId())
                    .sellerNickname(product.getSellerNickname())
                    .status(product.getStatus().toString())
                    .buyerId(buyerId)
                    .buyerNickname(buyerNickname)
                    .purchasePrice(product.getPrice().intValue())
                    .build();
            
            // 이벤트 발행도 비동기로 처리
            eventPublisherService.publishProductPurchasedAsync(purchaseEvent);
            
            log.debug("상품 구매 후처리 완료: {}", product.getId());
            
        } catch (Exception e) {
            log.error("상품 구매 후처리 중 오류 발생 (상품 ID: {}): {}", product.getId(), e.getMessage());
            // 후처리 실패해도 구매는 성공으로 처리
        }
    }

    public List<MyProductResponse> getMyProducts(String token) {
        Long sellerId = jwtService.extractUserId(token);
        
        // 🚀 성능 최적화: 읽기 전용 트랜잭션 + 인덱스 활용
        List<Product> products = productRepository.findBySellerId(sellerId);
        
        return products.stream()
            .map(product -> new MyProductResponse(
                product.getId(),
                product.getTitle(),
                product.getPrice(),
                product.getStatus().toString(),
                product.getImageUrl()
            ))
            .collect(Collectors.toList());
    }

    /**
     * 🚀 초고속 내 상품 조회 (JWT 검증 최적화)
     */
    @Transactional(readOnly = true)
    public List<MyProductResponse> getMyProductsFast(String token) {
        try {
            // 토큰에서 직접 사용자 ID 추출 (검증 생략으로 속도 향상)
            Long sellerId = jwtService.extractUserIdFast(token);
            
            log.debug("🚀 빠른 내 상품 조회 시작: 사용자 {}", sellerId);
            long startTime = System.currentTimeMillis();
            
            List<Product> products = productRepository.findBySellerId(sellerId);
            
            List<MyProductResponse> result = products.stream()
                .map(product -> new MyProductResponse(
                    product.getId(),
                    product.getTitle(),
                    product.getPrice(),
                    product.getStatus().toString(),
                    product.getImageUrl()
                ))
                .collect(Collectors.toList());
            
            long endTime = System.currentTimeMillis();
            log.info("✅ 빠른 내 상품 조회 완료: {}ms ({}개)", (endTime - startTime), result.size());
            
            return result;
        } catch (Exception e) {
            log.error("❌ 빠른 내 상품 조회 실패", e);
            // 실패 시 기본 메서드로 폴백
            return getMyProducts(token);
        }
    }

    @Transactional
    public void updateProduct(Long productId, String token, ProductRequest request) {
        // 🚀 빠른 사용자 ID 추출 (캐시 우선)
        Long userId = jwtService.extractUserIdFast(token);
        if (userId == null) {
            userId = jwtService.extractUserId(token); // 폴백
        }
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));
        
        // 권한 검증: 자신의 상품인지 확인
        if (!product.isOwnedBy(userId)) {
            throw new RuntimeException("해당 상품을 수정할 권한이 없습니다.");
        }
        
        // 판매된 상품은 수정 불가
        if (product.isSold()) {
            throw new RuntimeException("이미 판매된 상품은 수정할 수 없습니다.");
        }
        
        // 비즈니스 메서드를 사용하여 상품 정보 업데이트
        product.updateProduct(
            request.getTitle(),
            request.getDescription(),
            request.getPrice(),
            request.getCategory(),
            request.getImageUrl()
        );
        
        Product savedProduct = productRepository.save(product);
        
        // 🚀 상품 수정 이벤트 발행 (비동기로 처리)
        processProductUpdatedAsync(savedProduct);
    }

    /**
     * 🚀 상품 수정 후 비동기 처리
     */
    @Async("productAsyncExecutor")
    public void processProductUpdatedAsync(Product product) {
        try {
            ProductEventDto updateEvent = ProductEventDto.builder()
                    .productId(product.getId())
                    .title(product.getTitle())
                    .description(product.getDescription())
                    .price(product.getPrice().intValue())
                    .category(product.getCategory())
                    .imageUrl(product.getImageUrl())
                    .sellerId(product.getSellerId())
                    .sellerNickname(product.getSellerNickname())
                    .status(product.getStatus().toString())
                    .build();
            
            eventPublisherService.publishProductUpdatedAsync(updateEvent);
            log.debug("상품 수정 후처리 완료: {}", product.getId());
            
        } catch (Exception e) {
            log.error("상품 수정 후처리 중 오류 발생 (상품 ID: {}): {}", product.getId(), e.getMessage());
        }
    }

    @Transactional
    public void deleteProduct(Long productId, String token) {
        // 🚀 빠른 사용자 ID 추출 (캐시 우선)
        Long userId = jwtService.extractUserIdFast(token);
        if (userId == null) {
            userId = jwtService.extractUserId(token); // 폴백
        }
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));
        
        // 권한 검증: 자신의 상품인지 확인
        if (!product.isOwnedBy(userId)) {
            throw new RuntimeException("해당 상품을 삭제할 권한이 없습니다.");
        }
        
        // 판매된 상품은 삭제 불가 (구매자가 있을 수 있음)
        if (product.isSold()) {
            throw new RuntimeException("이미 판매된 상품은 삭제할 수 없습니다.");
        }
        
        // 삭제 전에 상품 정보 복사 (비동기 처리용)
        Product productCopy = Product.builder()
                .id(product.getId())
                .title(product.getTitle())
                .description(product.getDescription())
                .price(product.getPrice())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .sellerId(product.getSellerId())
                .sellerNickname(product.getSellerNickname())
                .status(product.getStatus())
                .build();
        
        // 🚀 상품 삭제 (핵심 로직만 동기 처리)
        productRepository.delete(product);
        
        // 🚀 삭제 이벤트 발행 (비동기로 처리)
        processProductDeletedAsync(productCopy);
    }

    /**
     * 🚀 상품 삭제 후 비동기 처리
     */
    @Async("productAsyncExecutor")
    public void processProductDeletedAsync(Product product) {
        try {
            ProductEventDto deleteEvent = ProductEventDto.builder()
                    .productId(product.getId())
                    .title(product.getTitle())
                    .description(product.getDescription())
                    .price(product.getPrice().intValue())
                    .category(product.getCategory())
                    .imageUrl(product.getImageUrl())
                    .sellerId(product.getSellerId())
                    .sellerNickname(product.getSellerNickname())
                    .status(product.getStatus().toString())
                    .build();
            
            eventPublisherService.publishProductDeletedAsync(deleteEvent);
            log.debug("상품 삭제 후처리 완료: {}", product.getId());
            
        } catch (Exception e) {
            log.error("상품 삭제 후처리 중 오류 발생 (상품 ID: {}): {}", product.getId(), e.getMessage());
        }
    }

    public List<PurchasedProductResponse> getPurchasedProducts(String token) {
        Long buyerId = jwtService.extractUserId(token);
        
        // 🚀 성능 최적화: 한 번의 쿼리로 구매 기록과 상품 정보를 함께 조회
        List<Purchase> purchases = purchaseRepository.findByUserIdOrderByPurchasedAtDesc(buyerId);
        
        if (purchases.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 상품 ID 목록 추출
        List<Long> productIds = purchases.stream()
                .map(Purchase::getProductId)
                .collect(Collectors.toList());
        
        // 한 번의 쿼리로 모든 상품 정보 조회
        Map<Long, Product> productMap = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
        
        // 결과 매핑 (N+1 문제 해결)
        return purchases.stream()
            .map(purchase -> {
                Product product = productMap.get(purchase.getProductId());
                
                return new PurchasedProductResponse(
                    purchase.getProductId(),
                    product != null ? product.getTitle() : "삭제된 상품",
                    purchase.getPurchasePrice(),
                    product != null ? product.getSellerNickname() : "탈퇴한 사용자",
                    purchase.getPurchasedAt(),
                    product != null ? product.getImageUrl() : null
                );
            })
            .collect(Collectors.toList());
    }

    /**
     * 🚀 초고속 구매 상품 조회 (JWT 검증 최적화)
     */
    @Transactional(readOnly = true)
    public List<PurchasedProductResponse> getPurchasedProductsFast(String token) {
        try {
            // 토큰에서 직접 사용자 ID 추출 (검증 생략으로 속도 향상)
            Long buyerId = jwtService.extractUserIdFast(token);
            if (buyerId == null) {
                log.warn("빠른 구매 상품 조회 실패 - 사용자 ID 추출 불가");
                return new ArrayList<>();
            }
            
            log.debug("🚀 빠른 구매 상품 조회 시작: 사용자 {}", buyerId);
            long startTime = System.currentTimeMillis();
            
            List<Purchase> purchases = purchaseRepository.findByUserIdOrderByPurchasedAtDesc(buyerId);
            
            if (purchases.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 상품 ID 목록 추출
            List<Long> productIds = purchases.stream()
                    .map(Purchase::getProductId)
                    .collect(Collectors.toList());
            
            // 한 번의 쿼리로 모든 상품 정보 조회
            Map<Long, Product> productMap = productRepository.findAllById(productIds)
                    .stream()
                    .collect(Collectors.toMap(Product::getId, product -> product));
            
            // 결과 매핑
            List<PurchasedProductResponse> result = purchases.stream()
                    .map(purchase -> {
                        Product product = productMap.get(purchase.getProductId());
                        
                        return new PurchasedProductResponse(
                            purchase.getProductId(),
                            product != null ? product.getTitle() : "삭제된 상품",
                            purchase.getPurchasePrice(),
                            product != null ? product.getSellerNickname() : "탈퇴한 사용자",
                            purchase.getPurchasedAt(),
                            product != null ? product.getImageUrl() : null
                        );
                    })
                    .collect(Collectors.toList());
            
            long endTime = System.currentTimeMillis();
            log.info("✅ 빠른 구매 상품 조회 완료: {}ms ({}개)", (endTime - startTime), result.size());
            
            return result;
        } catch (Exception e) {
            log.error("❌ 빠른 구매 상품 조회 실패", e);
            // 실패 시 기본 메서드로 폴백
            return getPurchasedProducts(token);
        }
    }

    /**
     * 상품 조회수 조회 (Analytics Service 연동 준비)
     */
    public long getProductViewCount(Long productId) {
        // TODO: Analytics Service에서 실제 조회수 가져오기
        // 현재는 구매 횟수 * 5로 임시 계산 (구매한 사람은 여러 번 봤을 것으로 가정)
        long purchaseCount = purchaseRepository.findByProductId(productId).size();
        return purchaseCount * 5 + (long)(Math.random() * 20); // 임시 조회수
    }

    public List<ProductResponse> getPopularProducts(int limit) {
        // 실제 인기도 기반 정렬: 구매 횟수 + 조회수 + 최신도 (가중치 적용)
        List<Product> allProducts = productRepository.findAll();
        
        return allProducts.stream()
            .filter(product -> product.getStatus() == Product.ProductStatus.AVAILABLE) // 판매 중인 상품만
            .map(product -> {
                // 구매 횟수 계산
                long purchaseCount = purchaseRepository.findByProductId(product.getId()).size();
                
                // 조회수 계산 (Analytics Service 연동 준비)
                long viewCount = getProductViewCount(product.getId());
                
                // 최신도 점수 (최근 30일 내 상품에 보너스)
                long daysSinceCreated = java.time.Duration.between(product.getCreatedAt(), java.time.LocalDateTime.now()).toDays();
                long recencyScore = Math.max(0, 30 - daysSinceCreated);
                
                // 인기도 점수 계산
                // - 구매 1회 = 50점 (가장 중요)
                // - 조회 1회 = 1점
                // - 최신도 = 최대 30점
                long popularityScore = purchaseCount * 50 + viewCount + recencyScore;
                
                return new ProductWithScore(product, popularityScore);
            })
            .sorted((a, b) -> Long.compare(b.getScore(), a.getScore())) // 점수 높은 순
            .limit(limit)
            .map(productWithScore -> new ProductResponse(
                productWithScore.getProduct().getId(),
                productWithScore.getProduct().getTitle(),
                productWithScore.getProduct().getPrice(),
                productWithScore.getProduct().getCategory(),
                productWithScore.getProduct().getImageUrl(),
                productWithScore.getProduct().getStatus().toString(),
                productWithScore.getProduct().getViewCount()
            ))
            .collect(Collectors.toList());
    }

    // 인기도 점수를 포함한 내부 클래스
    private static class ProductWithScore {
        private final Product product;
        private final long score;
        
        public ProductWithScore(Product product, long score) {
            this.product = product;
            this.score = score;
        }
        
        public Product getProduct() { return product; }
        public long getScore() { return score; }
    }

    /**
     * 대시보드 데이터 조회 (최고 성능 최적화 버전)
     */
    public Map<String, Object> getDashboardData() {
        Map<String, Object> dashboard = new HashMap<>();
        
        try {
            // 🚀 최고 성능 최적화: 최소한의 쿼리로 모든 통계 조회
            CompletableFuture<Long> totalProductsFuture = CompletableFuture.supplyAsync(() -> 
                productRepository.count());
            
            CompletableFuture<Long> totalPurchasesFuture = CompletableFuture.supplyAsync(() -> 
                purchaseRepository.count());
            
            // 상태별 통계를 한 번의 쿼리로 조회
            CompletableFuture<Map<String, Long>> statusStatsFuture = CompletableFuture.supplyAsync(() -> {
                List<Object[]> statusCounts = productRepository.countByStatusGrouped();
                Map<String, Long> statusMap = new HashMap<>();
                for (Object[] row : statusCounts) {
                    Product.ProductStatus status = (Product.ProductStatus) row[0];
                    Long count = ((Number) row[1]).longValue();
                    statusMap.put(status.toString(), count);
                }
                return statusMap;
            });
            
            // 카테고리별 통계를 한 번의 쿼리로 조회
            CompletableFuture<Map<String, Long>> categoryStatsFuture = CompletableFuture.supplyAsync(() -> {
                List<Object[]> categoryCounts = productRepository.countByCategoryGrouped();
                Map<String, Long> categoryMap = new HashMap<>();
                for (Object[] row : categoryCounts) {
                    String category = (String) row[0];
                    Long count = ((Number) row[1]).longValue();
                    categoryMap.put(category, count);
                }
                return categoryMap;
            });
            
            // 모든 비동기 작업 완료 대기
            CompletableFuture.allOf(
                totalProductsFuture, totalPurchasesFuture, statusStatsFuture, categoryStatsFuture
            ).join();
            
            // 결과 수집
            Map<String, Long> statusStats = statusStatsFuture.get();
            
            dashboard.put("totalProducts", totalProductsFuture.get());
            dashboard.put("soldProducts", statusStats.getOrDefault("SOLD", 0L));
            dashboard.put("availableProducts", statusStats.getOrDefault("AVAILABLE", 0L));
            dashboard.put("totalPurchases", totalPurchasesFuture.get());
            dashboard.put("categoryStats", categoryStatsFuture.get());
            dashboard.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            log.error("대시보드 데이터 조회 중 오류 발생", e);
            // 오류 발생 시 기본값 반환
            dashboard.put("totalProducts", 0L);
            dashboard.put("soldProducts", 0L);
            dashboard.put("availableProducts", 0L);
            dashboard.put("totalPurchases", 0L);
            dashboard.put("categoryStats", new HashMap<>());
            dashboard.put("error", "대시보드 데이터 조회 중 오류가 발생했습니다");
        }
        
        return dashboard;
    }

    public SseEmitter createProductStream() {
        // 🚀 타임아웃을 5분으로 늘려서 연결 안정성 향상
        SseEmitter emitter = new SseEmitter(300000L); // 5분 타임아웃
        String emitterId = "emitter-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
        
        try {
            log.info("🔗 SSE 연결 생성: {} (5분 타임아웃)", emitterId);
            
            // 연결 관리 맵에 추가
            productStreamEmitters.put(emitterId, emitter);
            
            // 🚀 연결 종료 시 정리 (상세 로깅 추가)
            emitter.onCompletion(() -> {
                log.info("✅ SSE 연결 정상 완료: {} (활성 연결: {}개)", emitterId, productStreamEmitters.size());
                productStreamEmitters.remove(emitterId);
            });
            
            emitter.onTimeout(() -> {
                log.warn("⏰ SSE 연결 타임아웃: {} (5분 경과, 활성 연결: {}개)", emitterId, productStreamEmitters.size());
                productStreamEmitters.remove(emitterId);
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.debug("타임아웃 시 연결 종료 중 오류 (무시 가능): {}", e.getMessage());
                }
            });
            
            emitter.onError((ex) -> {
                log.error("❌ SSE 연결 오류: {} - {} (활성 연결: {}개)", emitterId, ex.getMessage(), productStreamEmitters.size());
                productStreamEmitters.remove(emitterId);
                try {
                    emitter.completeWithError(ex);
                } catch (Exception e) {
                    log.debug("오류 시 연결 종료 중 추가 오류 (무시 가능): {}", e.getMessage());
                }
            });
            
            // 🚀 초기 데이터 전송 (간소화)
            try {
                List<ProductResponse> products = getProducts(null);
                emitter.send(SseEmitter.event()
                        .name("products")
                        .data(products)
                        .id(emitterId)
                        .comment("초기 상품 목록"));
                
                log.info("📡 SSE 초기 데이터 전송 완료: {} (상품 {}개, 활성 연결: {}개)", 
                    emitterId, products.size(), productStreamEmitters.size());
                
                // 🚀 연결 유지를 위한 하트비트 전송
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("connected")
                        .comment("연결 확인"));
                        
            } catch (IOException e) {
                log.error("❌ SSE 초기 데이터 전송 실패: {}", emitterId, e);
                productStreamEmitters.remove(emitterId);
                emitter.completeWithError(e);
                return emitter;
            }
            
        } catch (Exception e) {
            log.error("❌ SSE 연결 생성 중 오류: {}", emitterId, e);
            productStreamEmitters.remove(emitterId);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }

    /**
     * 상품 조회수 추적
     */
    public void trackProductView(Long productId, String category, Long userId, HttpServletRequest request) {
        try {
            log.info("🔍 상품 조회수 추적 시작 - 상품 ID: {}, 카테고리: {}, 사용자 ID: {}", productId, category, userId);
            
            AnalyticsEventDto analytics = AnalyticsEventDto.builder()
                    .eventType("VIEW")
                    .productId(productId)
                    .productCategory(category)
                    .userId(userId)
                    .sessionId(request.getSession().getId())
                    .userAgent(request.getHeader("User-Agent"))
                    .ipAddress(getClientIpAddress(request))
                    .referrer(request.getHeader("Referer"))
                    .currentPage(request.getRequestURL().toString())
                    .build();

            // 비동기로 분석 이벤트 발행
            eventPublisherService.publishViewAnalytics(analytics);
            log.info("✅ Analytics 이벤트 발행 완료 - 상품 ID: {}", productId);
        } catch (Exception e) {
            log.error("❌ 상품 조회수 추적 중 오류 발생 - 상품 ID: {}", productId, e);
        }
    }

    /**
     * 사용자별 상품 통계 조회 (초고속 최적화 버전 + 최근활동 포함)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserProductStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            log.debug("🚀 사용자 {} 통계 조회 시작", userId);
            long startTime = System.currentTimeMillis();
            
            // 🚀 병렬 처리로 모든 통계를 동시에 조회
            CompletableFuture<Long> registeredCountFuture = CompletableFuture.supplyAsync(() -> 
                productRepository.countBySellerId(userId));
            
            CompletableFuture<Long> purchasedCountFuture = CompletableFuture.supplyAsync(() -> 
                purchaseRepository.countByUserId(userId));
            
            CompletableFuture<Long> soldCountFuture = CompletableFuture.supplyAsync(() -> 
                productRepository.countBySellerIdAndStatus(userId, Product.ProductStatus.SOLD));
            
            // 거래 금액은 간단한 쿼리로 최적화
            CompletableFuture<Long> totalSalesAmountFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return purchaseRepository.findBySellerId(userId).stream()
                            .mapToLong(purchase -> purchase.getPurchasePrice().longValue())
                            .sum();
                } catch (Exception e) {
                    log.warn("판매 금액 계산 실패: {}", e.getMessage());
                    return 0L;
                }
            });
            
            CompletableFuture<Long> totalPurchaseAmountFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return purchaseRepository.findByUserId(userId).stream()
                            .mapToLong(purchase -> purchase.getPurchasePrice().longValue())
                            .sum();
                } catch (Exception e) {
                    log.warn("구매 금액 계산 실패: {}", e.getMessage());
                    return 0L;
                }
            });
            
            // 🆕 최근활동 정보 조회 (병렬 처리)
            CompletableFuture<List<Map<String, Object>>> recentActivitiesFuture = CompletableFuture.supplyAsync(() -> {
                List<Map<String, Object>> activities = new ArrayList<>();
                
                try {
                    // 최근 등록한 상품 (최대 3개)
                    List<Product> recentProducts = productRepository.findBySellerId(userId)
                            .stream()
                            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                            .limit(3)
                            .collect(Collectors.toList());
                    
                    for (Product product : recentProducts) {
                        Map<String, Object> activity = new HashMap<>();
                        activity.put("type", "PRODUCT_REGISTERED");
                        activity.put("title", product.getTitle());
                        activity.put("productId", product.getId());
                        activity.put("timestamp", product.getCreatedAt());
                        activity.put("description", "상품을 등록했습니다");
                        activities.add(activity);
                    }
                    
                    // 최근 구매한 상품 (최대 3개)
                    List<Purchase> recentPurchases = purchaseRepository.findByUserIdOrderByPurchasedAtDesc(userId)
                            .stream().limit(3).collect(Collectors.toList());
                    
                    for (Purchase purchase : recentPurchases) {
                        Map<String, Object> activity = new HashMap<>();
                        activity.put("type", "PRODUCT_PURCHASED");
                        activity.put("productId", purchase.getProductId());
                        activity.put("timestamp", purchase.getPurchasedAt());
                        activity.put("price", purchase.getPurchasePrice());
                        activity.put("description", "상품을 구매했습니다");
                        activities.add(activity);
                    }
                    
                    // 최근 판매된 상품 (최대 3개)
                    List<Purchase> recentSales = purchaseRepository.findBySellerId(userId)
                            .stream()
                            .sorted((a, b) -> b.getPurchasedAt().compareTo(a.getPurchasedAt()))
                            .limit(3)
                            .collect(Collectors.toList());
                    
                    for (Purchase sale : recentSales) {
                        Map<String, Object> activity = new HashMap<>();
                        activity.put("type", "PRODUCT_SOLD");
                        activity.put("productId", sale.getProductId());
                        activity.put("timestamp", sale.getPurchasedAt());
                        activity.put("price", sale.getPurchasePrice());
                        activity.put("description", "상품이 판매되었습니다");
                        activities.add(activity);
                    }
                    
                    // 시간순으로 정렬 (최신순)
                    activities.sort((a, b) -> {
                        LocalDateTime timeA = (LocalDateTime) a.get("timestamp");
                        LocalDateTime timeB = (LocalDateTime) b.get("timestamp");
                        return timeB.compareTo(timeA);
                    });
                    
                    // 최대 5개만 반환
                    return activities.stream().limit(5).collect(Collectors.toList());
                    
                } catch (Exception e) {
                    log.warn("최근활동 조회 실패: {}", e.getMessage());
                    return new ArrayList<>();
                }
            });
            
            // 모든 비동기 작업 완료 대기 (최대 5초)
            CompletableFuture.allOf(
                registeredCountFuture, purchasedCountFuture, soldCountFuture,
                totalSalesAmountFuture, totalPurchaseAmountFuture, recentActivitiesFuture
            ).get(5, TimeUnit.SECONDS);
            
            // 결과 수집
            long registeredCount = registeredCountFuture.get();
            long purchasedCount = purchasedCountFuture.get();
            long soldCount = soldCountFuture.get();
            long totalSalesAmount = totalSalesAmountFuture.get();
            long totalPurchaseAmount = totalPurchaseAmountFuture.get();
            List<Map<String, Object>> recentActivities = recentActivitiesFuture.get();
            
            stats.put("registeredCount", registeredCount);
            stats.put("purchasedCount", purchasedCount);
            stats.put("soldCount", soldCount);
            stats.put("totalSalesAmount", totalSalesAmount);
            stats.put("totalPurchaseAmount", totalPurchaseAmount);
            stats.put("totalTransactions", purchasedCount + soldCount);
            stats.put("recentActivities", recentActivities); // 🆕 최근활동 추가
            stats.put("timestamp", System.currentTimeMillis());
            
            long endTime = System.currentTimeMillis();
            log.info("✅ 사용자 {} 통계 조회 완료 - 소요시간: {}ms, 최근활동: {}개", 
                    userId, (endTime - startTime), recentActivities.size());
            
        } catch (Exception e) {
            log.error("❌ 사용자 상품 통계 조회 중 오류 발생: userId={}", userId, e);
            // 오류 발생 시 기본값 반환
            stats.put("registeredCount", 0L);
            stats.put("purchasedCount", 0L);
            stats.put("soldCount", 0L);
            stats.put("totalSalesAmount", 0L);
            stats.put("totalPurchaseAmount", 0L);
            stats.put("totalTransactions", 0L);
            stats.put("recentActivities", new ArrayList<>());
            stats.put("error", "통계 조회 중 오류가 발생했습니다");
        }
        
        return stats;
    }

    /**
     * 성능 최적화된 상품 상세 조회
     */
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetailFast(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));
        
        return new ProductDetailResponse(
            product.getId(),
            product.getTitle(),
            product.getDescription(),
            product.getPrice(),
            product.getCategory(),
            product.getImageUrl(),
            product.getSellerNickname(),
            product.getStatus().toString(),
            product.getViewCount()
        );
    }

    /**
     * 비동기 조회수 추적
     */
    public void trackProductViewAsync(Long productId, String category, String token) {
        // 별도 스레드에서 비동기 처리
        new Thread(() -> {
            try {
                Long userId = null;
                if (token != null && jwtService.validateToken(token)) {
                    userId = jwtService.extractUserId(token);
                }
                
                AnalyticsEventDto analytics = AnalyticsEventDto.builder()
                        .eventType("VIEW")
                        .productId(productId)
                        .productCategory(category)
                        .userId(userId)
                        .sessionId("async-" + System.currentTimeMillis())
                        .build();

                eventPublisherService.publishViewAnalytics(analytics);
                
            } catch (Exception e) {
                log.warn("비동기 조회수 추적 실패 (상품 ID: {}): {}", productId, e.getMessage());
            }
        }, "ProductView-" + productId).start();
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    // 실시간 스트림에 새 상품 즉시 반영
    private void broadcastNewProduct(Product product) {
        // 실시간 스트림에 새 상품 즉시 반영
        List<SseEmitter> emitters = new ArrayList<>(productStreamEmitters.values());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("products")
                        .data(new ProductResponse(
                            product.getId(),
                            product.getTitle(),
                            product.getPrice(),
                            product.getCategory(),
                            product.getImageUrl(),
                            product.getStatus().toString(),
                            product.getViewCount()
                        )));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }
    }
} 