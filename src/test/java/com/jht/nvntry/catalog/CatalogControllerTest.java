package com.jht.nvntry.catalog;

import com.jht.nvntry.catalog.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Best Practices for Spring Boot Integration Tests:
 * 1. Use @Testcontainers to manage DB lifecycle automatically.
 * 2. Use @SpringBootTest with RANDOM_PORT to test real HTTP stack.
 * 3. Use TestRestTemplate for simple HTTP calls or WebTestClient for reactive/advanced flows.
 * 4. Keep tests isolated: Clean DB state before/after each test class or method.
 * 5. Assert on HTTP Status Codes AND Response Body content.
 */
@Testcontainers
@SpringBootTest
@Sql(scripts = "/db/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@AutoConfigureMockMvc
public class CatalogControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("inventory")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Nested
    @DisplayName("Get Product")
    class GetProductTests {

        @Test
        @DisplayName("Should return 200 OK with product by ID")
        void shouldReturnProductById() throws Exception {
            var saved = productRepository.save(Product.create("GET-ID-SKU", "Get Me", Product.UnitOfMeasure.EACH));

            mockMvc.perform(get("/v1/products/{id}", saved.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                    .andExpect(jsonPath("$.sku").value("GET-ID-SKU"))
                    .andExpect(jsonPath("$.name").value("Get Me"))
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @DisplayName("Should return 404 Not Found for missing ID")
        void shouldReturnNotFoundById() throws Exception {
            mockMvc.perform(get("/v1/products/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 200 OK with product by SKU")
        void shouldReturnProductBySku() throws Exception {
            var saved = productRepository.save(Product.create("GET-SKU-NAME", "Get Me", Product.UnitOfMeasure.EACH));

            mockMvc.perform(get("/v1/products/sku/{sku}", saved.getSku()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                    .andExpect(jsonPath("$.sku").value("GET-SKU-NAME"))
                    .andExpect(jsonPath("$.name").value("Get Me"))
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @DisplayName("Should return 404 Not Found for missing SKU")
        void shouldReturnNotFoundBySku() throws Exception {
            mockMvc.perform(get("/v1/products/sku/{sku}", "RAND-SKU"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return active products")
        void shouldReturnProductWhenActiveExists() throws Exception {
            // Arrange: Create 2 active 1 inactive
            productRepository.save(Product.create("ACTIVE-1", "Active one", Product.UnitOfMeasure.EACH));
            productRepository.save(Product.create("ACTIVE-2", "Active two", Product.UnitOfMeasure.EACH));
            var inactive = Product.create("INACTIVE-1", "Inactive one", Product.UnitOfMeasure.EACH);
            inactive.setActive(false);
            productRepository.save(inactive);

            mockMvc.perform(get("/v1/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total_elements").value(2))
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].sku").value("ACTIVE-2"))
                    .andExpect(jsonPath("$.content[1].sku").value("ACTIVE-1"));
        }

        @Test
        @DisplayName("Should return empty when no active products exist")
        void shouldReturnEmptyWhenActiveNotExists() throws Exception {
            var inactive = Product.create("INACTIVE-1", "Inactive one", Product.UnitOfMeasure.EACH);
            inactive.setActive(false);
            productRepository.save(inactive);

            mockMvc.perform(get("/v1/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total_elements").value(0))
                    .andExpect(jsonPath("$.content.length()").value(0));
        }
    }

    @Nested
    @DisplayName("Create Product")
    class CreateProductTests {
        @Test
        @DisplayName("Should return 201 Created with Location header")
        void shouldCreateProduct() throws Exception {
            String request = """
                    {
                        "sku": "SKU-001",
                        "name": "Test Product",
                        "unit_of_measure": "EACH"
                    }
                    """;
            mockMvc.perform(post("/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.sku").value("SKU-001"))
                    .andExpect(jsonPath("$.name").value("Test Product"))
                    .andExpect(jsonPath("$.unit_of_measure").value("EACH"))
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @DisplayName("Should return 409 Conflict for duplicate SKU")
        void shouldReturnConflictForDuplicateSku() throws Exception {
            productRepository.save(Product.create("DUP-SKU", "Duplicate", Product.UnitOfMeasure.EACH));
            String request = """
                    {
                        "sku": "DUP-SKU",
                        "name": "Test Product",
                        "unit_of_measure": "EACH"
                    }
                    """;
            mockMvc.perform(post("/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("/errors/conflict"))
                    .andExpect(jsonPath("$.detail").value(containsString("DUP-SKU")));
        }

        @Test
        @DisplayName("Should return 400 Bad Request for invalid SKU")
        void shouldReturnBadRequestForInvalidSku() throws Exception {
            String request = """
                    {
                        "sku": "Invalid sku!",
                        "name": "Bad",
                        "unit_of_measure": "EACH"
                    }
                    """;
            mockMvc.perform(post("/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/errors/validation"))
                    .andExpect(jsonPath("$.detail").value("Validation failed"));
        }
    }

    @Nested
    @DisplayName("Update Product")
    class UpdateProductTests {
        @Test
        @DisplayName("Should return 200 OK when updating product name")
        void shouldUpdateProduct() throws Exception {
            var saved = productRepository.save(Product.create("SKU-001", "Update Me", Product.UnitOfMeasure.EACH));
            String request = """
                    {
                        "name": "Update Name"
                    }
                    """;
            mockMvc.perform(patch("/v1/products/{id}/name", saved.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sku").value("SKU-001"))
                    .andExpect(jsonPath("$.name").value("Update Name"))
                    .andExpect(jsonPath("$.unit_of_measure").value("EACH"))
                    .andExpect(jsonPath("$.active").value(true));

        }

        @Test
        @DisplayName("Should return 404 NOT FOUND when updating non existing product")
        void shouldErrorWhenUpdateProductNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            String request = """
                    {
                        "name": "Update Name"
                    }
                    """;
            mockMvc.perform(patch("/v1/products/{id}/name", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("/errors/not-found"))
                    .andExpect(jsonPath("$.detail").value(containsString(id.toString())));

        }
    }

    @Nested
    @DisplayName("Delete Product")
    class DeleteProductTests {
        @Test
        @DisplayName("Should return 204 NO CONTENT and delete product")
        void shouldDeleteProduct() throws Exception {
            var saved = productRepository.save(Product.create("DEL-SKU-NAME", "Del Me", Product.UnitOfMeasure.EACH));

            mockMvc.perform(delete("/v1/products/{id}", saved.getId()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 404 NOT FOUND on non existing")
        void shouldErrorWhenDeleteProductNotExisting() throws Exception {
            mockMvc.perform(delete("/v1/products/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }
    }
}