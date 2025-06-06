package com.minicarrot.product.controller;

import com.minicarrot.product.dto.*;
import com.minicarrot.product.service.ProductService;
import com.minicarrot.product.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final JwtService jwtService;

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
            @RequestParam(required = false) String category) {
        try {
            List<ProductResponse> products = productService.getProducts(category);
            return ResponseEntity.ok(ApiResponse.success(products));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("상품 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProduct(@PathVariable Long id) {
        try {
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
            // JWT 토큰 검증
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("유효하지 않은 토큰입니다."));
            }
            
            List<MyProductResponse> products = productService.getMyProducts(token);
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
            // JWT 토큰 검증
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("유효하지 않은 토큰입니다."));
            }
            
            List<PurchasedProductResponse> products = productService.getPurchasedProducts(token);
            return ResponseEntity.ok(ApiResponse.success(products));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("구매한 상품 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
} 