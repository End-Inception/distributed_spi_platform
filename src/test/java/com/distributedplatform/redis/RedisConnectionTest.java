package com.distributedplatform.redis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class RedisConnectionTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void shouldConnectToRedis() {

        redisTemplate.opsForValue().set(
                "test:redis",
                "connected"
        );

        String value = redisTemplate.opsForValue()
                .get("test:redis");

        assertEquals("connected", value);
    }
}