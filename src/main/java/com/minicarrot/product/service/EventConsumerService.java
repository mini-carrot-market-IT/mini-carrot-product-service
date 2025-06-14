package com.minicarrot.product.service;

import com.minicarrot.product.dto.event.AnalyticsEventDto;
import com.minicarrot.product.dto.event.NotificationEventDto;
import com.minicarrot.product.dto.event.ProductEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventConsumerService {

    private final NotificationService notificationService;
    private final AnalyticsService analyticsService;

    @RabbitListener(queues = "product.created.queue")
    public void handleProductCreated(ProductEventDto event) {
        log.info("Processing product created event: {}", event.getProductId());
        
        try {
            // 상품 생성 알림 발송
            notificationService.sendProductCreatedNotification(event);
            
            // 검색 인덱스 업데이트 (외부 서비스 호출)
            notificationService.updateSearchIndex(event);
            
            log.info("Successfully processed product created event: {}", event.getProductId());
        } catch (Exception e) {
            log.error("Failed to process product created event: {}", event.getProductId(), e);
        }
    }

    @RabbitListener(queues = "product.updated.queue")
    public void handleProductUpdated(ProductEventDto event) {
        log.info("Processing product updated event: {}", event.getProductId());
        
        try {
            // 관심 상품으로 등록한 사용자들에게 알림
            notificationService.sendProductUpdatedNotification(event);
            
            // 검색 인덱스 업데이트
            notificationService.updateSearchIndex(event);
            
            log.info("Successfully processed product updated event: {}", event.getProductId());
        } catch (Exception e) {
            log.error("Failed to process product updated event: {}", event.getProductId(), e);
        }
    }

    @RabbitListener(queues = "product.deleted.queue")
    public void handleProductDeleted(ProductEventDto event) {
        log.info("Processing product deleted event: {}", event.getProductId());
        
        try {
            // 관심 상품으로 등록한 사용자들에게 알림
            notificationService.sendProductDeletedNotification(event);
            
            // 검색 인덱스에서 제거
            notificationService.removeFromSearchIndex(event);
            
            log.info("Successfully processed product deleted event: {}", event.getProductId());
        } catch (Exception e) {
            log.error("Failed to process product deleted event: {}", event.getProductId(), e);
        }
    }

    @RabbitListener(queues = "product.purchased.queue")
    public void handleProductPurchased(ProductEventDto event) {
        log.info("Processing product purchased event: {}", event.getProductId());
        
        try {
            // 판매자에게 구매 알림
            notificationService.sendPurchaseNotificationToSeller(event);
            
            // 구매자에게 구매 확인 알림
            notificationService.sendPurchaseConfirmationToBuyer(event);
            
            // 재고 관리 시스템에 알림 (외부 서비스)
            notificationService.updateInventory(event);
            
            log.info("Successfully processed product purchased event: {}", event.getProductId());
        } catch (Exception e) {
            log.error("Failed to process product purchased event: {}", event.getProductId(), e);
        }
    }

    @RabbitListener(queues = "notification.email.queue")
    public void handleEmailNotification(NotificationEventDto notification) {
        log.info("Processing email notification: {}", notification.getTitle());
        
        try {
            // 실제 이메일 발송 로직
            notificationService.sendEmail(notification);
            
            log.info("Successfully sent email notification: {}", notification.getTitle());
        } catch (Exception e) {
            log.error("Failed to send email notification: {}", notification.getTitle(), e);
        }
    }

    @RabbitListener(queues = "notification.push.queue")
    public void handlePushNotification(NotificationEventDto notification) {
        log.info("Processing push notification: {}", notification.getTitle());
        
        try {
            // 실제 푸시 알림 발송 로직
            notificationService.sendPushNotification(notification);
            
            log.info("Successfully sent push notification: {}", notification.getTitle());
        } catch (Exception e) {
            log.error("Failed to send push notification: {}", notification.getTitle(), e);
        }
    }

    @RabbitListener(queues = "analytics.view.queue")
    public void handleViewAnalytics(AnalyticsEventDto analytics) {
        log.debug("Processing view analytics: {}", analytics.getProductId());
        
        try {
            // 분석 데이터 저장
            analyticsService.saveViewAnalytics(analytics);
            
            // 실시간 통계 업데이트
            analyticsService.updateRealTimeStats(analytics);
            
        } catch (Exception e) {
            log.error("Failed to process view analytics: {}", analytics.getProductId(), e);
        }
    }

    @RabbitListener(queues = "analytics.search.queue")
    public void handleSearchAnalytics(AnalyticsEventDto analytics) {
        log.debug("Processing search analytics: {}", analytics.getSearchKeyword());
        
        try {
            // 검색 분석 데이터 저장
            analyticsService.saveSearchAnalytics(analytics);
            
            // 인기 검색어 업데이트
            analyticsService.updatePopularKeywords(analytics);
            
        } catch (Exception e) {
            log.error("Failed to process search analytics: {}", analytics.getSearchKeyword(), e);
        }
    }
} 