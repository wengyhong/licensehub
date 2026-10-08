package com.weng.licensehub.product.api;
import java.security.Principal;
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
    ProductResponse create(@Valid @RequestBody CreateProductRequest request, Principal principal)
    {
        Product product = productService.createForOwner(principal.getName(), request.name(), request.description());
        return toResponse(product);
    }

    @GetMapping
    List<ProductResponse> findAll(Principal principal)
    {

       return productService.findAll(principal.getName()).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    ProductResponse findById(@PathVariable UUID id, Principal principal)
    {
       var response = productService.findById(id, principal.getName());

       return toResponse(response);
    }

    private ProductResponse toResponse(Product product)
    {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getCreatedAt(), product.getUpdatedAt());
    }
}
