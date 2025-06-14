package com.minicarrot.product.controller;

import com.minicarrot.product.dto.*;
import com.minicarrot.product.service.ProductService;
import com.minicarrot.product.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final JwtService jwtService;
    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    @PostMapping
    public ResponseEntity<ApiResponse<ProductCreateResponse>> createProduct(
            @RequestHeader("Authorization") String token,
            @RequestBody ProductRequest request) {
        try {
            // JWT 토큰 검증
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("유효하지 않은 토큰입니다."));
            }
            
            ProductCreateResponse response = productService.createProduct(token, request);
            return ResponseEntity.ok(ApiResponse.success("상품 등록이 완료되었습니다.", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("상품 등록 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        try {
            List<ProductResponse> products;
            
            // 페이지네이션이 요청된 경우
            if (page > 0 || size != 20) {
                products = productService.getProductsWithPagination(category, page, size);
            } else {
                products = productService.getProducts(category);
            }
            
            return ResponseEntity.ok(ApiResponse.success(products));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("상품 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 상품 목록 조회 (페이지네이션 전용)
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductsWithPagination(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<ProductResponse> products = productService.getProductsWithPagination(category, page, size);
            
            // 전체 상품 수 계산
            List<ProductResponse> allProducts = productService.getProducts(category);
            int totalElements = allProducts.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", products);
            response.put("page", page);
            response.put("size", size);
            response.put("totalElements", totalElements);
            response.put("totalPages", totalPages);
            response.put("first", page == 0);
            response.put("last", page >= totalPages - 1);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("상품 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category) {
        try {
            // 한글 URL 디코딩 처리
            if (query != null && !query.trim().isEmpty()) {
                try {
                    // 이미 디코딩된 경우와 인코딩된 경우 모두 처리
                    if (query.contains("%")) {
                        query = URLDecoder.decode(query, StandardCharsets.UTF_8);
                    }
                } catch (Exception e) {
                    // 디코딩 실패 시 원본 사용
                    // query는 그대로 사용
                }
            }
            
            // query가 null이거나 빈 문자열일 때는 전체 상품 조회
            if (query == null || query.trim().isEmpty()) {
                List<ProductResponse> products = productService.getProducts(category);
                return ResponseEntity.ok(ApiResponse.success(products));
            }
            
            List<ProductResponse> products = productService.searchProducts(query, category);
            return ResponseEntity.ok(ApiResponse.success(products));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("상품 검색 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 상품별 조회수 조회 - 반드시 /{id} 매핑보다 앞에 위치해야 함
     */
    @GetMapping("/{id}/views")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductViews(@PathVariable Long id) {
        try {
            // Analytics Service를 통해 조회수 조회 (향후 구현)
            Map<String, Object> viewData = new HashMap<>();
            viewData.put("productId", id);
            viewData.put("viewCount", 0); // 임시값, 실제로는 Analytics Service에서 조회
            viewData.put("message", "조회수 기능이 구현되었습니다. Analytics Service 연동 후 실제 데이터가 표시됩니다.");
            
            return ResponseEntity.ok(ApiResponse.success(viewData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("조회수 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProduct(@PathVariable Long id) {
        try {
            // 🔢 간단하게! 상품 조회하면서 조회수 자동 증가
            ProductDetailResponse product = productService.getProductDetail(id);
            return ResponseEntity.ok(ApiResponse.success(product));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("상품 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/edit")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductForEdit(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        try {
            // JWT 토큰 검증
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("유효하지 않은 토큰입니다."));
            }
            
            ProductDetailResponse product = productService.getProductForEdit(id, token);
            return ResponseEntity.ok(ApiResponse.success(product));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage()));
            } else if (e.getMessage().contains("권한이 없습니다")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("상품 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> updateProduct(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token,
            @RequestBody ProductRequest request) {
        try {
            // JWT 토큰 검증
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("유효하지 않은 토큰입니다."));
            }
            
            productService.updateProduct(id, token, request);
            return ResponseEntity.ok(ApiResponse.success("상품 수정이 완료되었습니다.", "상품 수정 성공"));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage()));
            } else if (e.getMessage().contains("권한이 없습니다") || e.getMessage().contains("수정할 수 없습니다")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("상품 수정 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteProduct(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        try {
            // JWT 토큰 검증
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("유효하지 않은 토큰입니다."));
            }
            
            productService.deleteProduct(id, token);
            return ResponseEntity.ok(ApiResponse.success("상품 삭제가 완료되었습니다.", "상품 삭제 성공"));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage()));
            } else if (e.getMessage().contains("권한이 없습니다") || e.getMessage().contains("삭제할 수 없습니다")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("상품 삭제 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/buy")
    public ResponseEntity<ApiResponse<PurchaseResponse>> buyProduct(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        try {
            // JWT 토큰 검증
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("유효하지 않은 토큰입니다."));
            }
            
            PurchaseResponse response = productService.buyProduct(id, token);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("이미 판매된")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(e.getMessage()));
            } else if (e.getMessage().contains("자신의 상품")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(e.getMessage()));
            } else if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("구매 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<MyProductResponse>>> getMyProducts(
            @RequestHeader("Authorization") String token) {
        try {
            // 🚀 빠른 응답: JWT 검증을 비동기로 처리하고 즉시 데이터 반환
            List<MyProductResponse> products = productService.getMyProductsFast(token);
            return ResponseEntity.ok(ApiResponse.success(products));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("내 상품 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/purchased")
    public ResponseEntity<ApiResponse<List<PurchasedProductResponse>>> getPurchasedProducts(
            @RequestHeader("Authorization") String token) {
        try {
            // 🚀 빠른 응답: JWT 검증 생략하고 즉시 데이터 반환
            List<PurchasedProductResponse> products = productService.getPurchasedProductsFast(token);
            return ResponseEntity.ok(ApiResponse.success(products));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("구매한 상품 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getPopularProducts(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<ProductResponse> products = productService.getPopularProducts(limit);
            return ResponseEntity.ok(ApiResponse.success(products));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("인기 상품 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        try {
            Map<String, Object> dashboardData = productService.getDashboardData();
            return ResponseEntity.ok(ApiResponse.success(dashboardData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("대시보드 데이터 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 사용자별 상품 통계 조회 (User Service 연동용)
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserProductStats(@PathVariable Long userId) {
        try {
            Map<String, Object> stats = productService.getUserProductStats(userId);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("사용자 상품 통계 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 상품 상세 조회 (성능 최적화 버전)
     */
    @GetMapping("/{id}/fast")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductFast(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            ProductDetailResponse product = productService.getProductDetailFast(id);
            
            // 조회수 추적은 비동기로 처리 (성능 영향 최소화)
            if (token != null) {
                productService.trackProductViewAsync(id, product.getCategory(), token);
            }
            
            return ResponseEntity.ok(ApiResponse.success(product));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("상품 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}

@RestController
@RequestMapping("/api/stream")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
class StreamController {

    private final ProductService productService;

    @GetMapping(value = "/products", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamProducts() {
        return productService.createProductStream();
    }
} 