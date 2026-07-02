package com.zynboot.app.controller;

import com.zynboot.infra.redis.RedisClient;
import com.zynboot.kit.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/api/v1/demo/redis")
public class RedisDemoController {

    private RedisClient redisClient;

    @Autowired(required = false)
    public void setRedisClient(RedisClient rc) { this.redisClient = rc; }

    @PutMapping
    public ApiResponse<String> set(@RequestParam String key, @RequestParam String value) {
        if (redisClient == null) return ApiResponse.fail("Redis not configured");
        redisClient.put(key, value, Duration.ofMinutes(30));
        return ApiResponse.ok("OK");
    }

    @GetMapping
    public ApiResponse<String> get(@RequestParam String key) {
        if (redisClient == null) return ApiResponse.fail("Redis not configured");
        return ApiResponse.ok(redisClient.get(key).orElse(null));
    }

    @DeleteMapping
    public ApiResponse<Boolean> delete(@RequestParam String key) {
        if (redisClient == null) return ApiResponse.fail("Redis not configured");
        return ApiResponse.ok(redisClient.delete(key));
    }
}
