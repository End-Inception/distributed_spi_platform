package com.distributedplatform.product;

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

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
        assertEquals(new BigDecimal("75000"), result.getPrice());

        verify(productRepository, times(1))
                .save(any(Product.class));
    }

    @Test
    void shouldGetProductById() {

        UUID id = UUID.randomUUID();

        Product product = new Product(
                "Gaming Laptop",
                "High performance laptop",
                new BigDecimal("75000")
        );

        when(productRepository.findById(id))
                .thenReturn(Optional.of(product));

        Product result = productService.getProductById(id);

        assertNotNull(result);
        assertEquals("Gaming Laptop", result.getName());

        verify(productRepository, times(1))
                .findById(id);
    }

    @Test
    void shouldThrowExceptionWhenProductNotFound() {

        UUID id = UUID.randomUUID();

        when(productRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> productService.getProductById(id)
        );

        verify(productRepository, times(1))
                .findById(id);
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

        List<Product> result = productService.getAllProducts();

        assertEquals(2, result.size());
        assertEquals("Gaming Laptop", result.get(0).getName());
        assertEquals("Mechanical Keyboard", result.get(1).getName());

        verify(productRepository, times(1))
                .findAll();
    }
}