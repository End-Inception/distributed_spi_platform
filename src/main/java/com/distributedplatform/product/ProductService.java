package com.distributedplatform.product;

import com.distributedplatform.cache.ProductCacheService;
import com.distributedplatform.exception.ProductNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCacheService productCacheService;

    public ProductService(
            ProductRepository productRepository,
            ProductCacheService productCacheService) {

        this.productRepository = productRepository;
        this.productCacheService = productCacheService;
    }

    public Product createProduct(
            String name,
            String description,
            BigDecimal price) {

        Product product = new Product(
                name,
                description,
                price
        );

        Product savedProduct =
                productRepository.save(product);

        productCacheService.put(savedProduct);

        return savedProduct;
    }

    public Product getProductById(UUID id) {

        // 1. Check Redis
        Product cachedProduct =
                productCacheService.get(id);

        if (cachedProduct != null) {
            return cachedProduct;
        }

        // 2. Cache miss → query PostgreSQL
        Product product =
                productRepository.findById(id)
                        .orElseThrow(() ->
                                new ProductNotFoundException(
                                        "Product not found"
                                ));

        // 3. Store result in Redis
        productCacheService.put(product);

        // 4. Return product
        return product;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product updateProduct(
        UUID id,
        String name,
        String description,
        BigDecimal price) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException("Product not found"));

        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);

        Product updatedProduct =
                productRepository.save(product);

        productCacheService.evict(id);

        return updatedProduct;
    }

    public void deleteProduct(UUID id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException("Product not found"));

        productRepository.delete(product);

        productCacheService.evict(id);
    }



}