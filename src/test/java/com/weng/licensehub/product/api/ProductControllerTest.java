package com.weng.licensehub.product.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.weng.licensehub.product.application.ProductService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.weng.licensehub.product.domain.Product;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.weng.licensehub.product.application.ProductNotFoundException;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import java.util.List;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import static org.mockito.Mockito.when;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private ProductService productService;

        @Test
        void createReturns201AndCreatedProduct() throws Exception {
                UUID id = UUID.fromString("7a38cd7d-e02b-4ed7-bb88-31f284d85a34");
                Instant timestamp = Instant.parse("2026-09-23T10:00:00Z");

                Product product = mock(Product.class);
                when(product.getId()).thenReturn(id);
                when(product.getName()).thenReturn("LicenseHub Desktop");
                when(product.getDescription()).thenReturn("Desktop product");
                when(product.getCreatedAt()).thenReturn(timestamp);
                when(product.getUpdatedAt()).thenReturn(timestamp);

                when(productService.createForOwner(
                                "owner@example.com",
                                "LicenseHub Desktop",
                                "Desktop product")).thenReturn(product);

                mockMvc.perform(post("/api/products")
                                .principal(() -> "owner@example.com")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "name": "LicenseHub Desktop",
                                                  "description": "Desktop product"
                                                }
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(id.toString()))
                                .andExpect(jsonPath("$.name").value("LicenseHub Desktop"))
                                .andExpect(jsonPath("$.description").value("Desktop product"))
                                .andExpect(jsonPath("$.createdAt").value(timestamp.toString()))
                                .andExpect(jsonPath("$.updatedAt").value(timestamp.toString()));
        }

        @Test
        void createReturns400WhenNameIsBlank() throws Exception {
                mockMvc.perform(post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "name": " ",
                                                  "description": "Invalid product"
                                                }
                                                """))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(productService);
        }

        @Test
        void findByIdReturns200WhenProductExists() throws Exception {
                UUID id = UUID.fromString("7a38cd7d-e02b-4ed7-bb88-31f284d85a34");
                Instant timestamp = Instant.parse("2026-09-23T10:00:00Z");

                Product product = mock(Product.class);
                when(product.getId()).thenReturn(id);
                when(product.getName()).thenReturn("LicenseHub Desktop");
                when(product.getDescription()).thenReturn("Desktop product");
                when(product.getCreatedAt()).thenReturn(timestamp);
                when(product.getUpdatedAt()).thenReturn(timestamp);
                when(productService.findById(
                                id,
                                "owner@example.com"))
                                .thenReturn(product);

                mockMvc.perform(get("/api/products/{id}", id)
                                .principal(() -> "owner@example.com"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(id.toString()))
                                .andExpect(jsonPath("$.name").value("LicenseHub Desktop"));
        }

        @Test
        void findByIdReturns404WhenProductDoesNotExist() throws Exception {
                UUID id = UUID.fromString("7a38cd7d-e02b-4ed7-bb88-31f284d85a34");

                when(productService.findById(
                                id,
                                "owner@example.com"))
                                .thenThrow(new ProductNotFoundException(id));

                mockMvc.perform(get("/api/products/{id}", id).principal(() -> "owner@example.com"))
                                .andExpect(status().isNotFound())
                                .andExpect(content().contentTypeCompatibleWith(
                                                MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.title").value("Product not found"))
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.detail")
                                                .value("Product '" + id + "' was not found"))
                                .andExpect(jsonPath("$.instance")
                                                .value("/api/products/" + id));
        }

        @Test
        void findAllReturns200AndProducts() throws Exception {
                UUID id = UUID.fromString("7a38cd7d-e02b-4ed7-bb88-31f284d85a34");
                Instant timestamp = Instant.parse("2026-09-23T10:00:00Z");

                Product product = mock(Product.class);
                when(product.getId()).thenReturn(id);
                when(product.getName()).thenReturn("LicenseHub Desktop");
                when(product.getDescription()).thenReturn("Desktop product");
                when(product.getCreatedAt()).thenReturn(timestamp);
                when(product.getUpdatedAt()).thenReturn(timestamp);

                when(productService.findAll(
                                "owner@example.com"))
                                .thenReturn(List.of(product));

                mockMvc.perform(get("/api/products")
                                .principal(() -> "owner@example.com"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].id").value(id.toString()))
                                .andExpect(jsonPath("$[0].name").value("LicenseHub Desktop"));
        }

        @Test
        void updateReturns200AndUpdatedProduct() throws Exception {
                UUID id = UUID.fromString("7a38cd7d-e02b-4ed7-bb88-31f284d85a34");
                Instant createdAt = Instant.parse("2026-09-23T10:00:00Z");
                Instant updatedAt = Instant.parse("2026-10-08T10:00:00Z");

                Product product = mock(Product.class);
                when(product.getId()).thenReturn(id);
                when(product.getName()).thenReturn("Updated Product");
                when(product.getDescription()).thenReturn("Updated description");
                when(product.getCreatedAt()).thenReturn(createdAt);
                when(product.getUpdatedAt()).thenReturn(updatedAt);

                when(productService.updateForOwner(
                                id,
                                "owner@example.com",
                                "Updated Product",
                                "Updated description"))
                                .thenReturn(product);

                mockMvc.perform(put("/api/products/{productId}", id)
                                .principal(() -> "owner@example.com")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "name": "Updated Product",
                                                  "description": "Updated description"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(id.toString()))
                                .andExpect(jsonPath("$.name").value("Updated Product"))
                                .andExpect(jsonPath("$.description").value("Updated description"))
                                .andExpect(jsonPath("$.createdAt").value(createdAt.toString()))
                                .andExpect(jsonPath("$.updatedAt").value(updatedAt.toString()));

                verify(productService).updateForOwner(
                                id,
                                "owner@example.com",
                                "Updated Product",
                                "Updated description");
        }

        @Test
        void updateReturns400WhenNameIsBlank() throws Exception {
                UUID id = UUID.fromString("7a38cd7d-e02b-4ed7-bb88-31f284d85a34");

                mockMvc.perform(put("/api/products/{productId}", id)
                                .principal(() -> "owner@example.com")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "name": " ",
                                                  "description": "Description"
                                                }
                                                """))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(productService);
        }
}
