package com.zynboot.infra.kafka.config;

import com.zynboot.infra.kafka.KafkaClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaOperations;

@AutoConfiguration
@ConditionalOnProperty(prefix = "zyn.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnSingleCandidate(KafkaOperations.class)
    public KafkaClient kafkaClient(KafkaOperations<Object, Object> operations) {
        KafkaClient client = new KafkaClient();
        client.setOperations(operations);
        return client;
    }
}
