package com.weng.licensehub.product.application;

import java.util.UUID;

public class ProductHasLicensesException extends RuntimeException{

    public ProductHasLicensesException(UUID productId)
    {
        super(String.format("Product '%s' has existing licenses", productId));
    }
}
