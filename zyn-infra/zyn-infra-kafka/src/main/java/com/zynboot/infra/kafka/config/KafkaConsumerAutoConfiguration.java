package com.zynboot.infra.kafka.config;

import com.zynboot.infra.kafka.KafkaConsumerSupport;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;

@AutoConfiguration
@ConditionalOnBean(ConsumerFactory.class)
@ConditionalOnProperty(prefix = "zyn.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConsumerAutoConfiguration {

    @Bean
    @ConditionalOnBean(KafkaOperations.class)
    @ConditionalOnSingleCandidate(KafkaOperations.class)
    @ConditionalOnMissingBean(ConsumerRecordRecoverer.class)
    public ConsumerRecordRecoverer kafkaConsumerRecordRecoverer(KafkaOperations<Object, Object> kafkaOperations) {
        return new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition())
        );
    }

    @Bean
    @ConditionalOnMissingBean(CommonErrorHandler.class)
    public CommonErrorHandler kafkaCommonErrorHandler(ObjectProvider<ConsumerRecordRecoverer> recovererProvider) {
        ConsumerRecordRecoverer recoverer = recovererProvider.getIfAvailable();
        if (recoverer == null) {
            return new DefaultErrorHandler();
        }
        return new DefaultErrorHandler(recoverer);
    }

    @Bean
    @ConditionalOnMissingBean
    public KafkaConsumerSupport kafkaConsumerSupport() {
        return new KafkaConsumerSupport();
    }
}
