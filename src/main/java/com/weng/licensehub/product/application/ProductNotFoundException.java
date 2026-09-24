package com.weng.licensehub.product.application;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(UUID id)
    {
        super(String.format("Product '%s' was not found", id));
    }
}
