package com.weng.licensehub.product.api;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.weng.licensehub.product.application.ProductService;
import com.weng.licensehub.product.domain.Product;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService service)
    {
        this.productService = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ProductResponse create(@Valid @RequestBody CreateProductRequest request)
    {
        Product product = productService.create(request.name(), request.description());
        return toResponse(product);
    }

    @GetMapping
    List<ProductResponse> findAll()
    {
        List<Product> listOfProducts = productService.findAll();
       return listOfProducts.stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    ResponseEntity<ProductResponse> findById(@PathVariable UUID id)
    {
       var response = productService.findById(id).map(this::toResponse);

       return ResponseEntity.of(response);
    }

    private ProductResponse toResponse(Product product)
    {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getCreatedAt(), product.getUpdatedAt());
    }
}
