package com.minicarrot.product.service;

import com.minicarrot.product.dto.*;
import com.minicarrot.product.entity.Product;
import com.minicarrot.product.entity.Purchase;
import com.minicarrot.product.repository.ProductRepository;
import com.minicarrot.product.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final JwtService jwtService;

    @Transactional
    public ProductCreateResponse createProduct(String token, ProductRequest request) {
        Long sellerId = jwtService.extractUserId(token);
        String sellerNickname = jwtService.extractNickname(token);
        
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
        
        return new ProductCreateResponse(savedProduct.getId(), savedProduct.getStatus().toString());
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

    public ProductDetailResponse getProductDetail(Long id) {
        Product product = productRepository.findById(id)
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
        productRepository.save(product);
        
        return new PurchaseResponse("구매 완료되었습니다.", productId);
    }

    public List<MyProductResponse> getMyProducts(String token) {
        Long sellerId = jwtService.extractUserId(token);
        
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
        
        // 상품 삭제
        productRepository.delete(product);
    }

    public List<PurchasedProductResponse> getPurchasedProducts(String token) {
        Long buyerId = jwtService.extractUserId(token);
        
        List<Purchase> purchases = purchaseRepository.findByUserIdOrderByPurchasedAtDesc(buyerId);
        
        return purchases.stream()
            .map(purchase -> {
                Product product = productRepository.findById(purchase.getProductId())
                    .orElse(null);
                
                return new PurchasedProductResponse(
                    purchase.getProductId(),
                    product != null ? product.getTitle() : "삭제된 상품",
                    product != null ? product.getPrice() : null,
                    product != null ? product.getSellerNickname() : "탈퇴한 사용자",
                    purchase.getPurchasedAt(),
                    product != null ? product.getImageUrl() : null
                );
            })
            .collect(Collectors.toList());
    }
} 