package com.weng.licensehub.product.api;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.weng.licensehub.product.application.ProductService;
import com.weng.licensehub.product.domain.Product;
import com.weng.licensehub.shared.api.PagedResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService service) {
        this.productService = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ProductResponse create(@Valid @RequestBody CreateProductRequest request, Principal principal) {
        Product product = productService.createForOwner(principal.getName(), request.name(), request.description());
        return toResponse(product);
    }

    @GetMapping
    PagedResponse<ProductResponse> findAll(Principal principal, @PageableDefault (size = 20, sort ="createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return PagedResponse.from(productService.findAll(principal.getName(), pageable).map(this::toResponse));
    }

    @GetMapping("/{id}")
    ProductResponse findById(@PathVariable UUID id, Principal principal) {
        var response = productService.findById(id, principal.getName());

        return toResponse(response);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getCreatedAt(),
                product.getUpdatedAt());
    }

    @PutMapping("/{productId}")
    ProductResponse updateById(
            @PathVariable UUID productId,
            @RequestBody @Valid UpdateProductRequest requestBody,
            Principal principal) {

        Product product = productService.updateForOwner(productId, principal.getName(), requestBody.name(),
                requestBody.description());

        return toResponse(product);

    }

    @DeleteMapping("/{productId}")
    ResponseEntity<Void> deleteById(
            @PathVariable UUID productId,
            Principal principal)

    {
        productService.deleteForOwner(productId, principal.getName());

        return ResponseEntity.noContent().build();

    }

}
