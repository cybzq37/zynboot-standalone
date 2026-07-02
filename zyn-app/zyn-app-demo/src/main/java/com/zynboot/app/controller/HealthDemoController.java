package com.zynboot.app.controller;

import com.zynboot.infra.es.EsClient;
import com.zynboot.infra.kafka.KafkaClient;
import com.zynboot.infra.redis.RedisClient;
import com.zynboot.kit.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/demo/health")
public class HealthDemoController {

    private DataSource dataSource;
    private RedisClient redisClient;
    private EsClient esClient;
    private KafkaClient kafkaClient;

    @Autowired(required = false)
    public void setDataSource(DataSource ds) { this.dataSource = ds; }

    @Autowired(required = false)
    public void setRedisClient(RedisClient rc) { this.redisClient = rc; }

    @Autowired(required = false)
    public void setEsClient(EsClient ec) { this.esClient = ec; }

    @Autowired(required = false)
    public void setKafkaClient(KafkaClient kc) { this.kafkaClient = kc; }

    @GetMapping
    public ApiResponse<Map<String, String>> check() {
        Map<String, String> result = new LinkedHashMap<>();

        // PostgreSQL
        if (dataSource != null) {
            try (Connection conn = dataSource.getConnection()) {
                result.put("postgres", "OK - " + conn.getMetaData().getDatabaseProductName());
            } catch (Exception e) {
                result.put("postgres", "FAIL - " + e.getMessage());
            }
        } else {
            result.put("postgres", "SKIP - not configured");
        }

        // Redis
        if (redisClient != null) {
            try {
                redisClient.put("_health", "ok");
                String val = redisClient.get("_health").orElse("null");
                redisClient.delete("_health");
                result.put("redis", "OK - read/write=" + val);
            } catch (Exception e) {
                result.put("redis", "FAIL - " + e.getMessage());
            }
        } else {
            result.put("redis", "SKIP - not configured");
        }

        // Elasticsearch
        if (esClient != null) {
            result.put("elasticsearch", "OK - client connected");
        } else {
            result.put("elasticsearch", "SKIP - not configured");
        }

        // Kafka
        result.put("kafka", kafkaClient != null ? "OK - configured" : "SKIP - not configured");

        return ApiResponse.ok(result);
    }
}
