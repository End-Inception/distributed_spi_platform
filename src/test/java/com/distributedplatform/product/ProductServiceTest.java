package com.distributedplatform.product;

import com.distributedplatform.cache.ProductCacheService;
import com.distributedplatform.exception.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCacheService productCacheService;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldCreateProduct() {

        Product product = new Product(
                "Gaming Laptop",
                "High performance laptop",
                new BigDecimal("75000")
        );

        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        Product result = productService.createProduct(
                "Gaming Laptop",
                "High performance laptop",
                new BigDecimal("75000")
        );

        assertNotNull(result);
        assertEquals("Gaming Laptop", result.getName());
        assertEquals(
                new BigDecimal("75000"),
                result.getPrice()
        );

        verify(productRepository, times(1))
                .save(any(Product.class));

        verify(productCacheService, times(1))
                .put(product);
    }

    @Test
    void shouldReturnProductFromCache() {

        UUID id = UUID.randomUUID();

        Product cachedProduct = new Product(
                "Gaming Laptop",
                "High performance laptop",
                new BigDecimal("75000")
        );

        when(productCacheService.get(id))
                .thenReturn(cachedProduct);

        Product result =
                productService.getProductById(id);

        assertNotNull(result);
        assertEquals(
                "Gaming Laptop",
                result.getName()
        );

        verify(productCacheService, times(1))
                .get(id);

        // PostgreSQL should NOT be queried.
        verify(productRepository, never())
                .findById(any(UUID.class));
    }

    @Test
    void shouldGetProductFromDatabaseOnCacheMiss() {

        UUID id = UUID.randomUUID();

        Product product = new Product(
                "Gaming Laptop",
                "High performance laptop",
                new BigDecimal("75000")
        );

        when(productCacheService.get(id))
                .thenReturn(null);

        when(productRepository.findById(id))
                .thenReturn(Optional.of(product));

        Product result =
                productService.getProductById(id);

        assertNotNull(result);
        assertEquals(
                "Gaming Laptop",
                result.getName()
        );

        verify(productCacheService, times(1))
                .get(id);

        verify(productRepository, times(1))
                .findById(id);

        // Database result should be added to Redis.
        verify(productCacheService, times(1))
                .put(product);
    }

    @Test
    void shouldThrowExceptionWhenProductNotFound() {

        UUID id = UUID.randomUUID();

        when(productCacheService.get(id))
                .thenReturn(null);

        when(productRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> productService.getProductById(id)
        );

        verify(productCacheService, times(1))
                .get(id);

        verify(productRepository, times(1))
                .findById(id);

        // Nothing should be cached when product doesn't exist.
        verify(productCacheService, never())
                .put(any(Product.class));
    }

    @Test
    void shouldGetAllProducts() {

        Product product1 = new Product(
                "Gaming Laptop",
                "High performance laptop",
                new BigDecimal("75000")
        );

        Product product2 = new Product(
                "Mechanical Keyboard",
                "RGB mechanical keyboard",
                new BigDecimal("5000")
        );

        when(productRepository.findAll())
                .thenReturn(List.of(product1, product2));

        List<Product> result =
                productService.getAllProducts();

        assertEquals(2, result.size());
        assertEquals(
                "Gaming Laptop",
                result.get(0).getName()
        );
        assertEquals(
                "Mechanical Keyboard",
                result.get(1).getName()
        );

        verify(productRepository, times(1))
                .findAll();
    }

    @Test
    void shouldUpdateProductAndEvictCache() {

        UUID id = UUID.randomUUID();

        Product existingProduct = new Product(
                "Gaming Laptop",
                "High performance laptop",
                new BigDecimal("75000")
        );

        when(productRepository.findById(id))
                .thenReturn(Optional.of(existingProduct));

        when(productRepository.save(existingProduct))
                .thenReturn(existingProduct);

        Product result = productService.updateProduct(
                id,
                "Gaming Laptop Pro",
                "Updated high performance laptop",
                new BigDecimal("90000")
        );

        assertNotNull(result);

        assertEquals(
                "Gaming Laptop Pro",
                result.getName()
        );

        assertEquals(
                "Updated high performance laptop",
                result.getDescription()
        );

        assertEquals(
                new BigDecimal("90000"),
                result.getPrice()
        );

        verify(productRepository, times(1))
                .findById(id);

        verify(productRepository, times(1))
                .save(existingProduct);

        verify(productCacheService, times(1))
                .evict(id);

    }


    @Test
    void shouldThrowExceptionWhenUpdatingNonExistingProduct() {

        UUID id = UUID.randomUUID();

        when(productRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> productService.updateProduct(
                        id,
                        "Gaming Laptop Pro",
                        "Updated description",
                        new BigDecimal("90000")
                )
        );

        verify(productRepository, times(1))
                .findById(id);

        verify(productRepository, never())
                .save(any(Product.class));

        verify(productCacheService, never())
                .evict(any(UUID.class));
    }


    @Test
    void shouldDeleteProductAndEvictCache() {

        UUID id = UUID.randomUUID();

        Product product = new Product(
                "Gaming Laptop",
                "High performance laptop",
                new BigDecimal("75000")
        );

        when(productRepository.findById(id))
                .thenReturn(Optional.of(product));

        productService.deleteProduct(id);

        verify(productRepository, times(1))
                .findById(id);

        verify(productRepository, times(1))
                .delete(product);

        verify(productCacheService, times(1))
                .evict(id);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistingProduct() {

        UUID id = UUID.randomUUID();

        when(productRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> productService.deleteProduct(id)
        );

        verify(productRepository, times(1))
                .findById(id);

        verify(productRepository, never())
                .delete(any(Product.class));

        verify(productCacheService, never())
                .evict(any(UUID.class));
    }



}