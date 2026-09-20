package com.distributedplatform.cache;

import com.distributedplatform.product.Product;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProductCacheService {

    private static final String PRODUCT_KEY_PREFIX = "product:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public ProductCacheService(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void put(Product product) {
        try {
            String key = PRODUCT_KEY_PREFIX + product.getId();
            String value = objectMapper.writeValueAsString(product);

            redisTemplate.opsForValue().set(key, value);

        } catch (JsonProcessingException exception) {
            throw new RuntimeException("Failed to cache product", exception);
        }
    }

    public Product get(UUID productId) {
        try {
            String key = PRODUCT_KEY_PREFIX + productId;

            String value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                return null;
            }

            return objectMapper.readValue(value, Product.class);

        } catch (JsonProcessingException exception) {
            throw new RuntimeException(
                    "Failed to read product from cache",
                    exception
            );
        }
    }

    public void evict(UUID productId) {
        String key = PRODUCT_KEY_PREFIX + productId;
        redisTemplate.delete(key);
    }
}