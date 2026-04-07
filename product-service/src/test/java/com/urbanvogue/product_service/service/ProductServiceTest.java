package com.urbanvogue.product_service.service;

import com.urbanvogue.product_service.dto.ProductRequestDTO;
import com.urbanvogue.product_service.dto.ProductResponseDTO;
import com.urbanvogue.product_service.model.Product;
import com.urbanvogue.product_service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private ProductRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Urban Sneaker")
                .description("Premium streetwear sneaker")
                .price(129.99)
                .imageUrl("https://example.com/sneaker.jpg")
                .build();

        requestDTO = new ProductRequestDTO();
        requestDTO.setName("Urban Sneaker");
        requestDTO.setDescription("Premium streetwear sneaker");
        requestDTO.setPrice(129.99);
        requestDTO.setImageUrl("https://example.com/sneaker.jpg");
    }

    @Test
    @DisplayName("createProduct - Saves and returns correct DTO")
    void createProduct_Success() {
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponseDTO response = productService.createProduct(requestDTO);

        assertNotNull(response);
        assertEquals("Urban Sneaker", response.getName());
        assertEquals(129.99, response.getPrice());
        assertEquals("https://example.com/sneaker.jpg", response.getImageUrl());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("getAllProducts - Returns list of DTOs")
    void getAllProducts_ReturnsList() {
        Product product2 = Product.builder()
                .id(2L).name("Urban Jacket").description("Warm jacket")
                .price(89.99).imageUrl("https://example.com/jacket.jpg").build();

        when(productRepository.findAll()).thenReturn(List.of(product, product2));

        List<ProductResponseDTO> result = productService.getAllProducts();

        assertEquals(2, result.size());
        assertEquals("Urban Sneaker", result.get(0).getName());
        assertEquals("Urban Jacket", result.get(1).getName());
    }

    @Test
    @DisplayName("getProductById - Returns DTO when found")
    void getProductById_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponseDTO response = productService.getProductById(1L);

        assertEquals(1L, response.getId());
        assertEquals("Urban Sneaker", response.getName());
    }

    @Test
    @DisplayName("getProductById - Not found throws 404")
    void getProductById_NotFound_Throws404() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                productService.getProductById(999L));
    }

    @Test
    @DisplayName("updateProduct - Updates all fields and returns DTO")
    void updateProduct_Success() {
        ProductRequestDTO updateRequest = new ProductRequestDTO();
        updateRequest.setName("Updated Sneaker");
        updateRequest.setDescription("New description");
        updateRequest.setPrice(149.99);
        updateRequest.setImageUrl("https://example.com/new.jpg");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponseDTO response = productService.updateProduct(1L, updateRequest);

        assertEquals("Updated Sneaker", product.getName());
        assertEquals(149.99, product.getPrice());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("deleteProduct - Not found throws 404")
    void deleteProduct_NotFound_Throws404() {
        when(productRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResponseStatusException.class, () ->
                productService.deleteProduct(999L));

        verify(productRepository, never()).deleteById(anyLong());
    }
}
