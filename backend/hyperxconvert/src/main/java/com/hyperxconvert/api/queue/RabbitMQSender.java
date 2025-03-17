package com.hyperxconvert.api.queue;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitMQSender {
    private final RabbitTemplate rabbitTemplate;

    public void sendToQueue(String fileId, String filePath, String targetFormat) {
        FileMessage message = new FileMessage(fileId, filePath, targetFormat);
        rabbitTemplate.convertAndSend("file_exchange", "file_routing", message);
    }
}