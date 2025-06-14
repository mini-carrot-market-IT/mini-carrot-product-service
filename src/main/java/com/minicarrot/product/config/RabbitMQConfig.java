package com.minicarrot.product.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 상품 이벤트 관련 큐와 익스체인지
    public static final String PRODUCT_EXCHANGE = "product.exchange";
    public static final String PRODUCT_CREATED_QUEUE = "product.created.queue";
    public static final String PRODUCT_UPDATED_QUEUE = "product.updated.queue";
    public static final String PRODUCT_DELETED_QUEUE = "product.deleted.queue";
    public static final String PRODUCT_PURCHASED_QUEUE = "product.purchased.queue";
    
    // 알림 관련 큐와 익스체인지
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String EMAIL_NOTIFICATION_QUEUE = "notification.email.queue";
    public static final String PUSH_NOTIFICATION_QUEUE = "notification.push.queue";
    
    // 분석 관련 큐와 익스체인지
    public static final String ANALYTICS_EXCHANGE = "analytics.exchange";
    public static final String VIEW_ANALYTICS_QUEUE = "analytics.view.queue";
    public static final String SEARCH_ANALYTICS_QUEUE = "analytics.search.queue";

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public TopicExchange productExchange() {
        return new TopicExchange(PRODUCT_EXCHANGE);
    }

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public TopicExchange analyticsExchange() {
        return new TopicExchange(ANALYTICS_EXCHANGE);
    }

    // 상품 이벤트 큐들
    @Bean
    public Queue productCreatedQueue() {
        return QueueBuilder.durable(PRODUCT_CREATED_QUEUE).build();
    }

    @Bean
    public Queue productUpdatedQueue() {
        return QueueBuilder.durable(PRODUCT_UPDATED_QUEUE).build();
    }

    @Bean
    public Queue productDeletedQueue() {
        return QueueBuilder.durable(PRODUCT_DELETED_QUEUE).build();
    }

    @Bean
    public Queue productPurchasedQueue() {
        return QueueBuilder.durable(PRODUCT_PURCHASED_QUEUE).build();
    }

    // 알림 큐들
    @Bean
    public Queue emailNotificationQueue() {
        return QueueBuilder.durable(EMAIL_NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Queue pushNotificationQueue() {
        return QueueBuilder.durable(PUSH_NOTIFICATION_QUEUE).build();
    }

    // 분석 큐들
    @Bean
    public Queue viewAnalyticsQueue() {
        return QueueBuilder.durable(VIEW_ANALYTICS_QUEUE).build();
    }

    @Bean
    public Queue searchAnalyticsQueue() {
        return QueueBuilder.durable(SEARCH_ANALYTICS_QUEUE).build();
    }

    // 바인딩 설정
    @Bean
    public Binding productCreatedBinding() {
        return BindingBuilder
                .bind(productCreatedQueue())
                .to(productExchange())
                .with("product.created");
    }

    @Bean
    public Binding productUpdatedBinding() {
        return BindingBuilder
                .bind(productUpdatedQueue())
                .to(productExchange())
                .with("product.updated");
    }

    @Bean
    public Binding productDeletedBinding() {
        return BindingBuilder
                .bind(productDeletedQueue())
                .to(productExchange())
                .with("product.deleted");
    }

    @Bean
    public Binding productPurchasedBinding() {
        return BindingBuilder
                .bind(productPurchasedQueue())
                .to(productExchange())
                .with("product.purchased");
    }

    @Bean
    public Binding emailNotificationBinding() {
        return BindingBuilder
                .bind(emailNotificationQueue())
                .to(notificationExchange())
                .with("notification.email");
    }

    @Bean
    public Binding pushNotificationBinding() {
        return BindingBuilder
                .bind(pushNotificationQueue())
                .to(notificationExchange())
                .with("notification.push");
    }

    @Bean
    public Binding viewAnalyticsBinding() {
        return BindingBuilder
                .bind(viewAnalyticsQueue())
                .to(analyticsExchange())
                .with("analytics.view");
    }

    @Bean
    public Binding searchAnalyticsBinding() {
        return BindingBuilder
                .bind(searchAnalyticsQueue())
                .to(analyticsExchange())
                .with("analytics.search");
    }
} 