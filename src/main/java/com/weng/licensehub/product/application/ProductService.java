package com.weng.licensehub.product.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weng.licensehub.product.domain.Product;
import com.weng.licensehub.product.persistence.ProductRepository;
import com.weng.licensehub.user.domain.UserAccount;
import com.weng.licensehub.user.persistence.UserAccountRepository;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final UserAccountRepository userAccountRepository;

    public ProductService(ProductRepository repo,
            UserAccountRepository userAccountRepository) {

        this.repository = repo;
        this.userAccountRepository = userAccountRepository;
    }


    @Transactional
    public Product createForOwner(String email, String name, String description) {
        UserAccount owner = requireOwner(email);
        Product product = new Product(owner, name, description);

        repository.save(product);

        return product;
    }

    private UserAccount requireOwner(String email) {
        return userAccountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user account was not found"));

    }

    @Transactional(readOnly = true)
    public List<Product> findAll(String ownerEmail) {

        UserAccount owner = requireOwner(ownerEmail);

        return repository
                .findAllByOwner_IdOrderByCreatedAtDesc(
                        owner.getId());
    }

    @Transactional(readOnly = true)
    public Product findById(
            UUID productId,
            String ownerEmail) {

        UserAccount owner = requireOwner(ownerEmail);

        return repository
                .findByIdAndOwner_Id(
                        productId,
                        owner.getId())
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

}
