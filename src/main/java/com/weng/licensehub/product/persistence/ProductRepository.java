package com.weng.licensehub.product.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weng.licensehub.product.domain.Product;

public interface ProductRepository extends JpaRepository<Product, UUID> {


    List<Product> findAllByOwner_IdOrderByCreatedAtDesc(UUID ownerId);

    Optional<Product> findByIdAndOwner_Id(UUID productId, UUID ownerId);
}
