package com.urbanvogue.product_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urbanvogue.product_service.dto.ProductRequestDTO;
import com.urbanvogue.product_service.model.Product;
import com.urbanvogue.product_service.repository.ProductRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ProductService - Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        productRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("INT-01: POST /api/products - Create product and verify persistence in DB")
    @WithMockUser(roles = "ADMIN")
    void createProduct_ShouldPersistToDatabase() throws Exception {
        ProductRequestDTO request = new ProductRequestDTO();
        request.setName("Urban Sneaker Pro");
        request.setDescription("Premium leather sneaker");
        request.setPrice(149.99);
        request.setImageUrl("https://img.com/sneaker.jpg");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Urban Sneaker Pro"))
                .andExpect(jsonPath("$.price").value(149.99))
                .andExpect(jsonPath("$.id").isNumber());

        // Verify the product was actually saved in H2 database
        assertEquals(1, productRepository.count(), "Product should be persisted in DB");
        Product saved = productRepository.findAll().get(0);
        assertEquals("Urban Sneaker Pro", saved.getName());
        assertEquals(149.99, saved.getPrice());
        System.out.println("  [PASS] INT-01: Product created and persisted with id=" + saved.getId());
    }

    @Test
    @Order(2)
    @DisplayName("INT-02: GET /api/products/{id} - Fetch product from real DB")
    void getProductById_ShouldReturnFromDatabase() throws Exception {
        // Pre-seed DB
        Product product = Product.builder()
                .name("Classic Hoodie")
                .description("Cotton blend hoodie")
                .price(79.99)
                .imageUrl("https://img.com/hoodie.jpg")
                .build();
        product = productRepository.save(product);

        mockMvc.perform(get("/api/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Classic Hoodie"))
                .andExpect(jsonPath("$.price").value(79.99));

        System.out.println("  [PASS] INT-02: Product fetched from DB with id=" + product.getId());
    }

    @Test
    @Order(3)
    @DisplayName("INT-03: PUT /api/products/{id} - Update product and verify persistence")
    @WithMockUser(roles = "ADMIN")
    void updateProduct_ShouldUpdateInDatabase() throws Exception {
        // Pre-seed DB
        Product product = Product.builder()
                .name("Old Name")
                .description("Old Desc")
                .price(50.0)
                .build();
        product = productRepository.save(product);

        ProductRequestDTO updateReq = new ProductRequestDTO();
        updateReq.setName("Updated Sneaker");
        updateReq.setDescription("Updated premium sneaker");
        updateReq.setPrice(199.99);

        mockMvc.perform(put("/api/products/" + product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Sneaker"))
                .andExpect(jsonPath("$.price").value(199.99));

        // Verify DB was actually updated
        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertEquals("Updated Sneaker", updated.getName());
        assertEquals(199.99, updated.getPrice());
        System.out.println("  [PASS] INT-03: Product updated in DB, id=" + product.getId());
    }

    @Test
    @Order(4)
    @DisplayName("INT-04: DELETE /api/products/{id} - Delete and verify removal from DB")
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_ShouldRemoveFromDatabase() throws Exception {
        Product product = Product.builder()
                .name("To Delete")
                .description("This will be deleted")
                .price(10.0)
                .build();
        product = productRepository.save(product);
        Long id = product.getId();

        // Verify exists
        assertTrue(productRepository.existsById(id));

        mockMvc.perform(delete("/api/products/" + id))
                .andExpect(status().isOk());

        // Verify removed from DB
        assertFalse(productRepository.existsById(id), "Product should be deleted from DB");
        assertEquals(0, productRepository.count());
        System.out.println("  [PASS] INT-04: Product deleted from DB, id=" + id);
    }

    @Test
    @Order(5)
    @DisplayName("INT-05: GET /api/products - List all products from DB")
    void getAllProducts_ShouldReturnAllFromDatabase() throws Exception {
        productRepository.save(Product.builder().name("P1").description("D1").price(10.0).build());
        productRepository.save(Product.builder().name("P2").description("D2").price(20.0).build());
        productRepository.save(Product.builder().name("P3").description("D3").price(30.0).build());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("P1"))
                .andExpect(jsonPath("$[2].name").value("P3"));

        System.out.println("  [PASS] INT-05: All 3 products returned from DB");
    }

    @Test
    @Order(6)
    @DisplayName("INT-06: POST /api/products - Validation rejects invalid request")
    @WithMockUser(roles = "ADMIN")
    void createProduct_WithInvalidData_ShouldReturn400() throws Exception {
        ProductRequestDTO invalid = new ProductRequestDTO();
        // Missing required fields: name, description, price

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        assertEquals(0, productRepository.count(), "No product should be saved");
        System.out.println("  [PASS] INT-06: Validation rejected invalid product, DB unchanged");
    }
}
