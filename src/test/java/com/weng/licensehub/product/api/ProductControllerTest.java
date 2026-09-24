package com.weng.licensehub.product.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.weng.licensehub.product.application.ProductService;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

@WebMvcTest(ProductController.class)
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
        given(product.getId()).willReturn(id);
        given(product.getName()).willReturn("LicenseHub Desktop");
        given(product.getDescription()).willReturn("Desktop product");
        given(product.getCreatedAt()).willReturn(timestamp);
        given(product.getUpdatedAt()).willReturn(timestamp);

        given(productService.create(
                "LicenseHub Desktop",
                "Desktop product")).willReturn(product);

        mockMvc.perform(post("/api/products")
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
        given(product.getId()).willReturn(id);
        given(product.getName()).willReturn("LicenseHub Desktop");
        given(product.getDescription()).willReturn("Desktop product");
        given(product.getCreatedAt()).willReturn(timestamp);
        given(product.getUpdatedAt()).willReturn(timestamp);

        given(productService.findById(id))
                .willReturn((product));

        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("LicenseHub Desktop"));
    }

    @Test
    void findByIdReturns404WhenProductDoesNotExist() throws Exception {
        UUID id = UUID.fromString("7a38cd7d-e02b-4ed7-bb88-31f284d85a34");

        given(productService.findById(id))
                .willThrow(new ProductNotFoundException(
                        id));


        mockMvc.perform(get("/api/products/{id}", id))
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
        given(product.getId()).willReturn(id);
        given(product.getName()).willReturn("LicenseHub Desktop");
        given(product.getDescription()).willReturn("Desktop product");
        given(product.getCreatedAt()).willReturn(timestamp);
        given(product.getUpdatedAt()).willReturn(timestamp);

        given(productService.findAll())
                .willReturn(List.of(product));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].name").value("LicenseHub Desktop"));
    }
}
