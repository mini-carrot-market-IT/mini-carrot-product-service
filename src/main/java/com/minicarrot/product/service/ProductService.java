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
        // 1. JWT 토큰에서 사용자 정보 추출 (캐시된 정보 우선 사용)
        Long sellerId = jwtService.extractUserId(token);
        String sellerNickname = jwtService.extractNickname(token);
        
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

    public List<ProductResponse> getProducts(String category) {
        List<Product> products;
        
        if (category != null && !category.isEmpty()) {
            products = productRepository.findByCategoryOrderByCreatedAtDesc(category);
        } else {
            products = productRepository.findAllByOrderByCreatedAtDesc();
        }
        
        return products.stream()
            .map(product -> new ProductResponse(
                product.getId(),
                product.getTitle(),
                product.getPrice(),
                product.getCategory(),
                product.getImageUrl(),
                product.getStatus().toString()
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
                product.getStatus().toString()
            ))
            .collect(Collectors.toList());
    }

    public ProductDetailResponse getProductDetail(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));
        
        // 📈 상품 조회 분석 이벤트 발행
        AnalyticsEventDto viewAnalytics = AnalyticsEventDto.builder()
                .eventType("VIEW")
                .productId(product.getId())
                .productTitle(product.getTitle())
                .productCategory(product.getCategory())
                .productPrice(product.getPrice().intValue())
                .build();
        
        eventPublisherService.publishViewAnalytics(viewAnalytics);
        
        return new ProductDetailResponse(
            product.getId(),
            product.getTitle(),
            product.getDescription(),
            product.getPrice(),
            product.getCategory(),
            product.getImageUrl(),
            product.getSellerNickname(),
            product.getStatus().toString()
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
            product.getStatus().toString()
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

    @Transactional
    public void updateProduct(Long productId, String token, ProductRequest request) {
        Long userId = jwtService.extractUserId(token);
        
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
        
        productRepository.save(product);
        
        // ✏️ 상품 수정 이벤트 발행
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
        
        eventPublisherService.publishProductUpdated(updateEvent);
    }

    @Transactional
    public void deleteProduct(Long productId, String token) {
        Long userId = jwtService.extractUserId(token);
        
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
        
        // 🗑️ 상품 삭제 이벤트 발행 (삭제 전에 발행)
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
        
        eventPublisherService.publishProductDeleted(deleteEvent);
        
        // 상품 삭제
        productRepository.delete(product);
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
                productWithScore.getProduct().getStatus().toString()
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
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        try {
            // 초기 데이터 전송
            List<ProductResponse> products = getProducts(null);
            emitter.send(SseEmitter.event()
                    .name("products")
                    .data(products));
            
            // 주기적으로 데이터 업데이트 (실제로는 이벤트 기반으로 처리)
            new Thread(() -> {
                try {
                    while (true) {
                        Thread.sleep(5000); // 5초마다 업데이트
                        List<ProductResponse> updatedProducts = getProducts(null);
                        emitter.send(SseEmitter.event()
                                .name("products")
                                .data(updatedProducts));
                    }
                } catch (IOException | InterruptedException e) {
                    emitter.completeWithError(e);
                }
            }).start();
            
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        
        return emitter;
    }

    /**
     * 상품 조회수 추적
     */
    public void trackProductView(Long productId, String category, Long userId, HttpServletRequest request) {
        try {
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
        } catch (Exception e) {
            log.error("상품 조회수 추적 중 오류 발생", e);
        }
    }

    /**
     * 사용자별 상품 통계 조회 (안정화 버전)
     */
    public Map<String, Object> getUserProductStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 등록한 상품 수
            long registeredCount = productRepository.countBySellerId(userId);
            
            // 구매한 상품 수
            long purchasedCount = purchaseRepository.countByUserId(userId);
            
            // 판매 완료된 상품 수
            long soldCount = productRepository.countBySellerIdAndStatus(userId, Product.ProductStatus.SOLD);
            
            // 총 거래 금액 (판매한 상품들의 총액)
            List<Purchase> soldPurchases = purchaseRepository.findBySellerId(userId);
            long totalSalesAmount = soldPurchases.stream()
                    .mapToLong(purchase -> purchase.getPurchasePrice().longValue())
                    .sum();
            
            // 총 구매 금액
            List<Purchase> purchases = purchaseRepository.findByUserId(userId);
            long totalPurchaseAmount = purchases.stream()
                    .mapToLong(purchase -> purchase.getPurchasePrice().longValue())
                    .sum();
            
            stats.put("registeredCount", registeredCount);
            stats.put("purchasedCount", purchasedCount);
            stats.put("soldCount", soldCount);
            stats.put("totalSalesAmount", totalSalesAmount);
            stats.put("totalPurchaseAmount", totalPurchaseAmount);
            stats.put("totalTransactions", purchasedCount + soldCount);
            stats.put("timestamp", System.currentTimeMillis());
            
            log.debug("사용자 {} 상품 통계: 등록 {}, 구매 {}, 판매 {}", userId, registeredCount, purchasedCount, soldCount);
            
        } catch (Exception e) {
            log.error("사용자 상품 통계 조회 중 오류 발생: userId={}", userId, e);
            // 오류 발생 시 기본값 반환
            stats.put("registeredCount", 0L);
            stats.put("purchasedCount", 0L);
            stats.put("soldCount", 0L);
            stats.put("totalSalesAmount", 0L);
            stats.put("totalPurchaseAmount", 0L);
            stats.put("totalTransactions", 0L);
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
            product.getStatus().toString()
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
                            product.getStatus().toString()
                        )));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }
    }
} 