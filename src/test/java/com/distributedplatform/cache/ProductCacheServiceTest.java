package com.distributedplatform.cache;

import com.distributedplatform.product.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductCacheServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ProductCacheService productCacheService;

    @BeforeEach
    void setUp() {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        productCacheService =
                new ProductCacheService(redisTemplate, objectMapper);
    }

    @Test
    void shouldPutProductInCache() {

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        Product product = new Product(
                "Gaming Laptop",
                "High performance laptop",
                new BigDecimal("75000")
        );

        productCacheService.put(product);

        verify(redisTemplate).opsForValue();

        verify(valueOperations).set(
                startsWith("product:"),
                contains("\"name\":\"Gaming Laptop\"")
        );
    }

    @Test
    void shouldReturnNullOnCacheMiss() {

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        UUID productId = UUID.randomUUID();

        when(valueOperations.get("product:" + productId))
                .thenReturn(null);

        Product result =
                productCacheService.get(productId);

        assertNull(result);

        verify(valueOperations)
                .get("product:" + productId);
    }

    @Test
    void shouldGetProductFromCache() {

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        UUID productId = UUID.randomUUID();

        String cachedProduct = """
                {
                  "id": "%s",
                  "name": "Gaming Laptop",
                  "description": "High performance laptop",
                  "price": 75000
                }
                """.formatted(productId);

        when(valueOperations.get("product:" + productId))
                .thenReturn(cachedProduct);

        Product result =
                productCacheService.get(productId);

        assertNotNull(result);

        assertEquals(
                "Gaming Laptop",
                result.getName()
        );

        assertEquals(
                "High performance laptop",
                result.getDescription()
        );

        assertEquals(
                new BigDecimal("75000"),
                result.getPrice()
        );

        verify(valueOperations)
                .get("product:" + productId);
    }

    @Test
    void shouldEvictProductFromCache() {

        UUID productId = UUID.randomUUID();

        productCacheService.evict(productId);

        verify(redisTemplate)
                .delete("product:" + productId);
    }
}