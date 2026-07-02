package com.zynboot.infra.redis;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RedisClientTest {

    @Test
    void shouldThrowWhenTemplateNotSet() {
        RedisClient client = new RedisClient();

        assertThatThrownBy(() -> client.put("k", "v"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Redis not configured");
    }

    @Test
    void shouldDelegatePutToTemplate() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);

        RedisClient client = new RedisClient();
        client.setRedisTemplate(template);

        client.put("key", "value");

        verify(ops).set("key", "value");
    }

    @Test
    void shouldDelegatePutWithTtl() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);

        RedisClient client = new RedisClient();
        client.setRedisTemplate(template);

        Duration ttl = Duration.ofMinutes(5);
        client.put("key", "value", ttl);

        verify(ops).set("key", "value", ttl);
    }

    @Test
    void shouldReturnOptionalWithValueOnGet() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(ops.get("key")).thenReturn("value");

        RedisClient client = new RedisClient();
        client.setRedisTemplate(template);

        Optional<String> result = client.get("key");

        assertThat(result).isPresent().contains("value");
    }

    @Test
    void shouldReturnEmptyOptionalWhenKeyMissing() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(ops.get("missing")).thenReturn(null);

        RedisClient client = new RedisClient();
        client.setRedisTemplate(template);

        Optional<String> result = client.get("missing");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldDelegateDelete() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.delete("key")).thenReturn(true);

        RedisClient client = new RedisClient();
        client.setRedisTemplate(template);

        Boolean result = client.delete("key");

        assertThat(result).isTrue();
        verify(template).delete("key");
    }

    @Test
    void shouldDelegateExists() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.hasKey("key")).thenReturn(true);

        RedisClient client = new RedisClient();
        client.setRedisTemplate(template);

        assertThat(client.exists("key")).isTrue();
    }

    @Test
    void shouldDelegateExpire() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.expire("key", Duration.ofSeconds(60))).thenReturn(true);

        RedisClient client = new RedisClient();
        client.setRedisTemplate(template);

        assertThat(client.expire("key", Duration.ofSeconds(60))).isTrue();
    }

    @Test
    void shouldDelegateIncrement() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(ops.increment("counter")).thenReturn(1L);

        RedisClient client = new RedisClient();
        client.setRedisTemplate(template);

        assertThat(client.increment("counter")).isEqualTo(1L);
    }

    @Test
    void shouldDelegateIncrementWithDelta() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(ops.increment("counter", 10L)).thenReturn(10L);

        RedisClient client = new RedisClient();
        client.setRedisTemplate(template);

        assertThat(client.increment("counter", 10L)).isEqualTo(10L);
    }

    @Test
    void shouldDelegateSetIfAbsent() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent("lock", "token", Duration.ofSeconds(30))).thenReturn(true);

        RedisClient client = new RedisClient();
        client.setRedisTemplate(template);

        assertThat(client.putIfAbsent("lock", "token", Duration.ofSeconds(30))).isTrue();
    }

    @Test
    void shouldReturnLockTokenOnSuccessfulTryLock() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        RedisClient client = new RedisClient();
        client.setRedisTemplate(template);

        Optional<String> token = client.tryLock("my-lock", Duration.ofSeconds(30));

        assertThat(token).isPresent();
        assertThat(token.get()).isNotBlank();
    }

    @Test
    void shouldReturnEmptyOnFailedTryLock() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        RedisClient client = new RedisClient();
        client.setRedisTemplate(template);

        Optional<String> token = client.tryLock("my-lock", Duration.ofSeconds(30));

        assertThat(token).isEmpty();
    }
}
