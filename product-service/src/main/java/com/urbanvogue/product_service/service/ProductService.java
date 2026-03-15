package com.urbanvogue.product_service.service;

import com.urbanvogue.product_service.dto.ProductRequestDTO;
import com.urbanvogue.product_service.dto.ProductResponseDTO;
import com.urbanvogue.product_service.model.Product;
import com.urbanvogue.product_service.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;


    public ProductResponseDTO createProduct(ProductRequestDTO request) {

        logger.info("Creating product with name: {}", request.getName());

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .build();

        Product saved = productRepository.save(product);

        logger.info("Product created successfully with id: {}", saved.getId());

        return ProductResponseDTO.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .price(saved.getPrice())
                .build();
    }


    public List<ProductResponseDTO> getAllProducts() {

        logger.info("Fetching all products");

        List<Product> products = productRepository.findAll();

        logger.info("Total products fetched: {}", products.size());

        return products.stream()
                .map(product -> ProductResponseDTO.builder()
                        .id(product.getId())
                        .name(product.getName())
                        .description(product.getDescription())
                        .price(product.getPrice())
                        .build())
                .toList();
    }


    public ProductResponseDTO getProductById(Long id) {

        logger.info("Fetching product with id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Product not found with id: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
                });

        logger.info("Product found: {}", product.getName());

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .build();
    }


    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO request) {

        logger.info("Updating product with id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Cannot update. Product not found with id: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
                });

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());

        Product updatedProduct = productRepository.save(product);

        logger.info("Product updated successfully with id: {}", updatedProduct.getId());

        return ProductResponseDTO.builder()
                .id(updatedProduct.getId())
                .name(updatedProduct.getName())
                .description(updatedProduct.getDescription())
                .price(updatedProduct.getPrice())
                .build();
    }


    public void deleteProduct(Long id) {

        logger.info("Deleting product with id: {}", id);

        if (!productRepository.existsById(id)) {

            logger.error("Cannot delete. Product not found with id: {}", id);

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }

        productRepository.deleteById(id);

        logger.info("Product deleted successfully with id: {}", id);
    }
}