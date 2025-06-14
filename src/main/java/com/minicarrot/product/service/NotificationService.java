package com.minicarrot.product.service;

import com.minicarrot.product.dto.event.NotificationEventDto;
import com.minicarrot.product.dto.event.ProductEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final WebClient.Builder webClientBuilder;

    public void sendProductCreatedNotification(ProductEventDto event) {
        log.info("Sending product created notification for product: {}", event.getProductId());
        
        // 주변 사용자들에게 새 상품 알림
        NotificationEventDto notification = NotificationEventDto.builder()
                .type("PUSH")
                .title("새로운 상품이 등록되었습니다!")
                .message(String.format("%s - %,d원", event.getTitle(), event.getPrice()))
                .productId(event.getProductId())
                .productTitle(event.getTitle())
                .productPrice(event.getPrice())
                .productImageUrl(event.getImageUrl())
                .priority("MEDIUM")
                .build();
        
        // 실제 알림 발송 로직 (예시)
        sendPushNotificationToNearbyUsers(notification);
    }

    public void sendProductUpdatedNotification(ProductEventDto event) {
        log.info("Sending product updated notification for product: {}", event.getProductId());
        
        // 관심 상품으로 등록한 사용자들에게 알림
        NotificationEventDto notification = NotificationEventDto.builder()
                .type("PUSH")
                .title("관심 상품이 업데이트되었습니다!")
                .message(String.format("%s - %,d원", event.getTitle(), event.getPrice()))
                .productId(event.getProductId())
                .productTitle(event.getTitle())
                .productPrice(event.getPrice())
                .productImageUrl(event.getImageUrl())
                .priority("HIGH")
                .build();
        
        sendPushNotificationToInterestedUsers(event.getProductId(), notification);
    }

    public void sendProductDeletedNotification(ProductEventDto event) {
        log.info("Sending product deleted notification for product: {}", event.getProductId());
        
        // 관심 상품으로 등록한 사용자들에게 알림
        NotificationEventDto notification = NotificationEventDto.builder()
                .type("PUSH")
                .title("관심 상품이 삭제되었습니다")
                .message(String.format("%s 상품이 더 이상 판매되지 않습니다", event.getTitle()))
                .productId(event.getProductId())
                .productTitle(event.getTitle())
                .priority("MEDIUM")
                .build();
        
        sendPushNotificationToInterestedUsers(event.getProductId(), notification);
    }

    public void sendPurchaseNotificationToSeller(ProductEventDto event) {
        log.info("Sending purchase notification to seller for product: {}", event.getProductId());
        
        NotificationEventDto notification = NotificationEventDto.builder()
                .type("PUSH")
                .recipient(event.getSellerId().toString())
                .title("상품이 판매되었습니다!")
                .message(String.format("%s 상품을 %s님이 구매했습니다", event.getTitle(), event.getBuyerNickname()))
                .productId(event.getProductId())
                .productTitle(event.getTitle())
                .productPrice(event.getPurchasePrice())
                .priority("HIGH")
                .build();
        
        sendPushNotification(notification);
        
        // 이메일도 함께 발송
        notification.setType("EMAIL");
        sendEmail(notification);
    }

    public void sendPurchaseConfirmationToBuyer(ProductEventDto event) {
        log.info("Sending purchase confirmation to buyer for product: {}", event.getProductId());
        
        NotificationEventDto notification = NotificationEventDto.builder()
                .type("PUSH")
                .recipient(event.getBuyerId().toString())
                .title("구매가 완료되었습니다!")
                .message(String.format("%s 상품 구매가 완료되었습니다", event.getTitle()))
                .productId(event.getProductId())
                .productTitle(event.getTitle())
                .productPrice(event.getPurchasePrice())
                .priority("HIGH")
                .build();
        
        sendPushNotification(notification);
        
        // 이메일 영수증 발송
        notification.setType("EMAIL");
        notification.setTemplateName("purchase_receipt");
        sendEmail(notification);
    }

    public void sendEmail(NotificationEventDto notification) {
        log.info("Sending email: {}", notification.getTitle());
        
        try {
            // 외부 이메일 서비스 API 호출 (예: SendGrid, AWS SES 등)
            WebClient webClient = webClientBuilder.build();
            
            webClient.post()
                    .uri("https://api.email-service.com/send")
                    .header("Authorization", "Bearer YOUR_API_KEY")
                    .bodyValue(notification)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(result -> log.info("Email sent successfully: {}", notification.getTitle()))
                    .doOnError(error -> log.error("Failed to send email: {}", notification.getTitle(), error))
                    .subscribe();
                    
        } catch (Exception e) {
            log.error("Error sending email notification", e);
        }
    }

    public void sendPushNotification(NotificationEventDto notification) {
        log.info("Sending push notification: {}", notification.getTitle());
        
        try {
            // 외부 푸시 알림 서비스 API 호출 (예: FCM, Apple Push 등)
            WebClient webClient = webClientBuilder.build();
            
            webClient.post()
                    .uri("https://fcm.googleapis.com/fcm/send")
                    .header("Authorization", "key=YOUR_SERVER_KEY")
                    .header("Content-Type", "application/json")
                    .bodyValue(notification)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(result -> log.info("Push notification sent successfully: {}", notification.getTitle()))
                    .doOnError(error -> log.error("Failed to send push notification: {}", notification.getTitle(), error))
                    .subscribe();
                    
        } catch (Exception e) {
            log.error("Error sending push notification", e);
        }
    }

    public void updateSearchIndex(ProductEventDto event) {
        log.info("Updating search index for product: {}", event.getProductId());
        
        try {
            // 검색 서비스 API 호출 (예: Elasticsearch, Solr 등)
            WebClient webClient = webClientBuilder.build();
            
            webClient.post()
                    .uri("https://search-service.com/index/products")
                    .header("Content-Type", "application/json")
                    .bodyValue(event)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(result -> log.info("Search index updated successfully: {}", event.getProductId()))
                    .doOnError(error -> log.error("Failed to update search index: {}", event.getProductId(), error))
                    .subscribe();
                    
        } catch (Exception e) {
            log.error("Error updating search index", e);
        }
    }

    public void removeFromSearchIndex(ProductEventDto event) {
        log.info("Removing from search index for product: {}", event.getProductId());
        
        try {
            WebClient webClient = webClientBuilder.build();
            
            webClient.delete()
                    .uri("https://search-service.com/index/products/{id}", event.getProductId())
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(result -> log.info("Product removed from search index: {}", event.getProductId()))
                    .doOnError(error -> log.error("Failed to remove from search index: {}", event.getProductId(), error))
                    .subscribe();
                    
        } catch (Exception e) {
            log.error("Error removing from search index", e);
        }
    }

    public void updateInventory(ProductEventDto event) {
        log.info("Updating inventory for product: {}", event.getProductId());
        
        try {
            // 재고 관리 서비스 API 호출
            WebClient webClient = webClientBuilder.build();
            
            webClient.post()
                    .uri("https://inventory-service.com/update")
                    .header("Content-Type", "application/json")
                    .bodyValue(event)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(result -> log.info("Inventory updated successfully: {}", event.getProductId()))
                    .doOnError(error -> log.error("Failed to update inventory: {}", event.getProductId(), error))
                    .subscribe();
                    
        } catch (Exception e) {
            log.error("Error updating inventory", e);
        }
    }

    private void sendPushNotificationToNearbyUsers(NotificationEventDto notification) {
        // 주변 사용자들에게 푸시 알림 발송 로직
        log.info("Sending push notification to nearby users: {}", notification.getTitle());
    }

    private void sendPushNotificationToInterestedUsers(Long productId, NotificationEventDto notification) {
        // 관심 상품으로 등록한 사용자들에게 푸시 알림 발송 로직
        log.info("Sending push notification to interested users for product: {}", productId);
    }
} 