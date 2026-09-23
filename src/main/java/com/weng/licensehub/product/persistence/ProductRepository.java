package com.weng.licensehub.product.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weng.licensehub.product.domain.Product;

public interface ProductRepository extends JpaRepository<Product, UUID> {

}
