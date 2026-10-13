package com.weng.licensehub.product.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weng.licensehub.product.domain.Product;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findAllByOwner_Id(
            UUID ownerId,
            Pageable pageable);

    Optional<Product> findByIdAndOwner_Id(UUID productId, UUID ownerId);
}
