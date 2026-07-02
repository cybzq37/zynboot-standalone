package com.zynboot.infra.kafka;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaOperations;

/**
 * Kafka 客户端封装。
 * <p>
 * 持有 {@link KafkaOperations} 引用，通过 {@link #getOperations()} 获取底层操作对象。
 * 由 {@link com.zynboot.infra.kafka.config.KafkaAutoConfiguration} 自动注册。
 */
public class KafkaClient {

    private KafkaOperations<Object, Object> operations;

    @Autowired(required = false)
    public void setOperations(KafkaOperations<Object, Object> operations) {
        this.operations = operations;
    }

    public KafkaOperations<Object, Object> getOperations() {
        if (operations == null) {
            throw new IllegalStateException("Kafka not configured, KafkaOperations not available");
        }
        return operations;
    }
}
