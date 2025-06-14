package com.minicarrot.product.service;

import com.minicarrot.product.config.RabbitMQConfig;
import com.minicarrot.product.dto.event.AnalyticsEventDto;
import com.minicarrot.product.dto.event.NotificationEventDto;
import com.minicarrot.product.dto.event.ProductEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisherService {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 비동기 상품 생성 이벤트 발행 (스프링 비동기 + 성능 최적화)
     */
    @Async("eventAsyncExecutor")
    public void publishProductCreatedAsync(ProductEventDto productEvent) {
        try {
            publishProductCreated(productEvent);
        } catch (Exception e) {
            log.error("비동기 상품 생성 이벤트 발행 실패: {}", productEvent.getProductId(), e);
        }
    }

    public void publishProductCreated(ProductEventDto productEvent) {
        try {
            productEvent.setEventType("CREATED");
            productEvent.setEventTime(LocalDateTime.now());
            productEvent.setEventId(UUID.randomUUID().toString());
            
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.PRODUCT_EXCHANGE,
                "product.created",
                productEvent
            );
            
            log.info("Published product created event: {}", productEvent.getProductId());
        } catch (Exception e) {
            log.error("Failed to publish product created event", e);
        }
    }

    /**
     * 비동기 상품 업데이트 이벤트 발행 (Spring @Async 사용)
     */
    @Async("eventAsyncExecutor")
    public void publishProductUpdatedAsync(ProductEventDto productEvent) {
        try {
            publishProductUpdated(productEvent);
        } catch (Exception e) {
            log.error("비동기 상품 업데이트 이벤트 발행 실패: {}", productEvent.getProductId(), e);
        }
    }

    public void publishProductUpdated(ProductEventDto productEvent) {
        try {
            productEvent.setEventType("UPDATED");
            productEvent.setEventTime(LocalDateTime.now());
            productEvent.setEventId(UUID.randomUUID().toString());
            
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.PRODUCT_EXCHANGE,
                "product.updated",
                productEvent
            );
            
            log.info("Published product updated event: {}", productEvent.getProductId());
        } catch (Exception e) {
            log.error("Failed to publish product updated event", e);
        }
    }

    /**
     * 비동기 상품 구매 이벤트 발행 (Spring @Async 사용)
     */
    @Async("eventAsyncExecutor")
    public void publishProductPurchasedAsync(ProductEventDto productEvent) {
        try {
            publishProductPurchased(productEvent);
        } catch (Exception e) {
            log.error("비동기 상품 구매 이벤트 발행 실패: {}", productEvent.getProductId(), e);
        }
    }

    /**
     * 비동기 상품 삭제 이벤트 발행 (Spring @Async 사용)
     */
    @Async("eventAsyncExecutor")
    public void publishProductDeletedAsync(ProductEventDto productEvent) {
        try {
            publishProductDeleted(productEvent);
        } catch (Exception e) {
            log.error("비동기 상품 삭제 이벤트 발행 실패: {}", productEvent.getProductId(), e);
        }
    }

    public void publishProductDeleted(ProductEventDto productEvent) {
        try {
            productEvent.setEventType("DELETED");
            productEvent.setEventTime(LocalDateTime.now());
            productEvent.setEventId(UUID.randomUUID().toString());
            
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.PRODUCT_EXCHANGE,
                "product.deleted",
                productEvent
            );
            
            log.info("Published product deleted event: {}", productEvent.getProductId());
        } catch (Exception e) {
            log.error("Failed to publish product deleted event", e);
        }
    }

    public void publishProductPurchased(ProductEventDto productEvent) {
        try {
            productEvent.setEventType("PURCHASED");
            productEvent.setEventTime(LocalDateTime.now());
            productEvent.setEventId(UUID.randomUUID().toString());
            
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.PRODUCT_EXCHANGE,
                "product.purchased",
                productEvent
            );
            
            log.info("Published product purchased event: {}", productEvent.getProductId());
        } catch (Exception e) {
            log.error("Failed to publish product purchased event", e);
        }
    }

    public void publishEmailNotification(NotificationEventDto notification) {
        try {
            notification.setNotificationId(UUID.randomUUID().toString());
            notification.setCreatedAt(LocalDateTime.now());
            
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                "notification.email",
                notification
            );
            
            log.info("Published email notification: {}", notification.getTitle());
        } catch (Exception e) {
            log.error("Failed to publish email notification", e);
        }
    }

    public void publishPushNotification(NotificationEventDto notification) {
        try {
            notification.setNotificationId(UUID.randomUUID().toString());
            notification.setCreatedAt(LocalDateTime.now());
            
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                "notification.push",
                notification
            );
            
            log.info("Published push notification: {}", notification.getTitle());
        } catch (Exception e) {
            log.error("Failed to publish push notification", e);
        }
    }

    public void publishViewAnalytics(AnalyticsEventDto analytics) {
        try {
            analytics.setEventId(UUID.randomUUID().toString());
            analytics.setTimestamp(LocalDateTime.now());
            
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ANALYTICS_EXCHANGE,
                "analytics.view",
                analytics
            );
            
            log.debug("Published view analytics: {}", analytics.getProductId());
        } catch (Exception e) {
            log.error("Failed to publish view analytics", e);
        }
    }

    public void publishSearchAnalytics(AnalyticsEventDto analytics) {
        try {
            analytics.setEventId(UUID.randomUUID().toString());
            analytics.setTimestamp(LocalDateTime.now());
            
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ANALYTICS_EXCHANGE,
                "analytics.search",
                analytics
            );
            
            log.debug("Published search analytics: {}", analytics.getSearchKeyword());
        } catch (Exception e) {
            log.error("Failed to publish search analytics", e);
        }
    }
} 