package com.zynboot.infra.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.Message;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class KafkaConsumerSupport {

    public boolean acknowledge(Acknowledgment acknowledgment) {
        if (acknowledgment == null) {
            return false;
        }
        acknowledgment.acknowledge();
        return true;
    }

    public Optional<String> topic(ConsumerRecord<?, ?> record) {
        return Optional.ofNullable(record).map(ConsumerRecord::topic);
    }

    public Optional<Integer> partition(ConsumerRecord<?, ?> record) {
        return Optional.ofNullable(record).map(ConsumerRecord::partition);
    }

    public Optional<Long> offset(ConsumerRecord<?, ?> record) {
        return Optional.ofNullable(record).map(ConsumerRecord::offset);
    }

    public Optional<String> keyAsString(ConsumerRecord<?, ?> record) {
        return Optional.ofNullable(record)
                .map(ConsumerRecord::key)
                .map(String::valueOf);
    }

    public Optional<String> header(ConsumerRecord<?, ?> record, String headerName) {
        return headerBytes(record, headerName)
                .map(value -> new String(value, StandardCharsets.UTF_8));
    }

    public Optional<byte[]> headerBytes(ConsumerRecord<?, ?> record, String headerName) {
        if (record == null) {
            return Optional.empty();
        }
        Header header = record.headers().lastHeader(headerName);
        return header == null ? Optional.empty() : Optional.ofNullable(header.value());
    }

    public Optional<String> topic(Message<?> message) {
        return header(message, KafkaHeaders.RECEIVED_TOPIC, String.class);
    }

    public Optional<Integer> partition(Message<?> message) {
        return header(message, KafkaHeaders.RECEIVED_PARTITION, Integer.class);
    }

    public Optional<Long> offset(Message<?> message) {
        return header(message, KafkaHeaders.OFFSET, Long.class);
    }

    public Optional<String> groupId(Message<?> message) {
        return header(message, KafkaHeaders.GROUP_ID, String.class);
    }

    public Optional<Object> key(Message<?> message) {
        return header(message, KafkaHeaders.RECEIVED_KEY, Object.class);
    }

    public <T> Optional<T> header(Message<?> message, String headerName, Class<T> type) {
        if (message == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(message.getHeaders().get(headerName, type));
    }
}
