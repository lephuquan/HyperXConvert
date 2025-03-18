package com.hyperxconvert.api.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Sender for RabbitMQ messages
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQSender {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange:hyperxconvert-exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key:file-conversion}")
    private String routingKey;

    @Value("${rabbitmq.queue.name:file-conversion-queue}")
    private String queueName;

    /**
     * Send a message to the queue
     *
     * @param message The message to send
     */
    public void sendMessage(FileMessage message) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            log.info("Sent message to queue: {}", message);
        } catch (Exception e) {
            log.error("Error sending message to queue", e);
            throw new RuntimeException("Failed to send message to queue", e);
        }
    }
}
