package com.ruoyi.web.controller.ai.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 */
@Configuration
public class RabbitMQConfig {

    // 交换机名称
    public static final String FILE_EXCHANGE = "file.exchange";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";

    // 队列名称
    public static final String FILE_VECTOR_QUEUE = "file.vector.queue";
    public static final String FILE_VECTOR_DLQ = "file.vector.dlq";
    public static final String EMAIL_NOTIFICATION_QUEUE = "email.notification.queue";

    // 路由键
    public static final String FILE_VECTOR_ROUTING_KEY = "file.vector";
    public static final String FILE_VECTOR_DLQ_ROUTING_KEY = "file.vector.dlq";
    public static final String EMAIL_ROUTING_KEY = "email.*";

    /**
     * 文件处理交换机
     */
    @Bean
    public DirectExchange fileExchange() {
        return new DirectExchange(FILE_EXCHANGE, true, false);
    }

    /**
     * 通知交换机
     */
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    /**
     * 文件向量化队列
     */
    @Bean
    public Queue fileVectorQueue() {
        return QueueBuilder.durable(FILE_VECTOR_QUEUE)
                .withArgument("x-dead-letter-exchange", FILE_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", FILE_VECTOR_DLQ_ROUTING_KEY)
                .withArgument("x-message-ttl", 300000) // 5分钟超时
                .build();
    }

    /**
     * 死信队列
     */
    @Bean
    public Queue fileVectorDLQ() {
        return QueueBuilder.durable(FILE_VECTOR_DLQ).build();
    }

    /**
     * 邮件通知队列
     */
    @Bean
    public Queue emailNotificationQueue() {
        return QueueBuilder.durable(EMAIL_NOTIFICATION_QUEUE).build();
    }

    /**
     * 绑定文件向量化队列
     */
    @Bean
    public Binding fileVectorBinding() {
        return BindingBuilder.bind(fileVectorQueue())
                .to(fileExchange())
                .with(FILE_VECTOR_ROUTING_KEY);
    }

    /**
     * 绑定死信队列
     */
    @Bean
    public Binding fileVectorDLQBinding() {
        return BindingBuilder.bind(fileVectorDLQ())
                .to(fileExchange())
                .with(FILE_VECTOR_DLQ_ROUTING_KEY);
    }

    /**
     * 绑定邮件队列
     */
    @Bean
    public Binding emailNotificationBinding() {
        return BindingBuilder.bind(emailNotificationQueue())
                .to(notificationExchange())
                .with(EMAIL_ROUTING_KEY);
    }

    /**
     * JSON 消息转换器
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate 配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                System.err.println("消息发送失败: " + cause);
            }
        });
        return template;
    }
}
