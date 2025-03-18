package com.hyperxconvert.api.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for RabbitMQ
 */
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.name:file-conversion-queue}")
    private String queueName;

    @Value("${rabbitmq.exchange:hyperxconvert-exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key:file-conversion}")
    private String routingKey;

    /**
     * Create a queue
     *
     * @return The queue
     */
    @Bean
    public Queue queue() {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", exchange + ".dlx")
                .withArgument("x-dead-letter-routing-key", routingKey + ".dlq")
                .build();
    }

    /**
     * Create a dead letter queue
     *
     * @return The dead letter queue
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(queueName + ".dlq").build();
    }

    /**
     * Create a topic exchange
     *
     * @return The exchange
     */
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchange);
    }

    /**
     * Create a dead letter exchange
     *
     * @return The dead letter exchange
     */
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(exchange + ".dlx");
    }

    /**
     * Bind the queue to the exchange
     *
     * @param queue The queue
     * @param exchange The exchange
     * @return The binding
     */
    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }

    /**
     * Bind the dead letter queue to the dead letter exchange
     *
     * @param deadLetterQueue The dead letter queue
     * @param deadLetterExchange The dead letter exchange
     * @return The binding
     */
    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(routingKey + ".dlq");
    }

    /**
     * Create a message converter
     *
     * @return The message converter
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Create a RabbitTemplate
     *
     * @param connectionFactory The connection factory
     * @return The RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
