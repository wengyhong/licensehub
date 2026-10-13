package com.weng.licensehub.product.persistence;

import com.weng.licensehub.product.domain.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {

  Page<Product> findAllByOwner_Id(UUID ownerId, Pageable pageable);

  Optional<Product> findByIdAndOwner_Id(UUID productId, UUID ownerId);
}
