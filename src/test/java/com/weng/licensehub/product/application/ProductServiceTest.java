package com.weng.licensehub.product.application;

import com.weng.licensehub.product.domain.Product;
import com.weng.licensehub.product.persistence.ProductRepository;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import java.util.UUID;

public class ProductServiceTest {

    private ProductRepository repository;
    private ProductService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        service = new ProductService(repository);
    }

    @Test
    void createSavesAndReturnsProduct() {
        given(repository.save(any(Product.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        Product result = service.create("LicenseHub Desktop", "Desktop product");

        assertEquals("LicenseHub Desktop", result.getName());
        assertEquals("Desktop product", result.getDescription());

        verify(repository).save(result);

    }

    @Test
    void getByIdThrowsWhenProductDoesNotExist() {
        UUID id = UUID.fromString(
                "7a38cd7d-e02b-4ed7-bb88-31f284d85a34");

        given(repository.findById(id))
                .willReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> service.findById(id));

        assertEquals(
                "Product '" + id + "' was not found",
                exception.getMessage());
    }
}
