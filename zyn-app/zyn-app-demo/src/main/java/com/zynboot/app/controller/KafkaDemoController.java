package com.zynboot.app.controller;

import com.zynboot.infra.kafka.KafkaClient;
import com.zynboot.kit.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@RestController
@RequestMapping("/api/v1/demo/kafka")
public class KafkaDemoController {

    private KafkaClient kafkaClient;
    private final CopyOnWriteArrayList<String> received = new CopyOnWriteArrayList<>();

    @Autowired(required = false)
    public void setKafkaClient(KafkaClient kc) { this.kafkaClient = kc; }

    @PostMapping("/send")
    public ApiResponse<String> send(@RequestParam(defaultValue = "demo-topic") String topic,
                                    @RequestParam String message) {
        if (kafkaClient == null) return ApiResponse.fail("Kafka not configured");
        kafkaClient.getOperations().send(topic, message);
        return ApiResponse.ok("sent");
    }

    @GetMapping("/messages")
    public ApiResponse<List<String>> messages() {
        return ApiResponse.ok(received);
    }

    /**
     * Kafka 消费者，仅在 Kafka 可用时启用。
     */
    @Component
    @ConditionalOnProperty(prefix = "spring.kafka", name = "bootstrap-servers")
    public static class DemoKafkaConsumer {

        private final CopyOnWriteArrayList<String> received;

        public DemoKafkaConsumer(KafkaDemoController controller) {
            this.received = controller.received;
        }

        @KafkaListener(topics = "demo-topic", groupId = "demo-group")
        public void onMessage(String message) {
            received.add(message);
            if (received.size() > 100) received.remove(0);
        }
    }
}
