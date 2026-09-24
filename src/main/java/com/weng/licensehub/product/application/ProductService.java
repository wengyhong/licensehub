package com.weng.licensehub.product.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weng.licensehub.product.domain.Product;
import com.weng.licensehub.product.persistence.ProductRepository;


@Service
public class ProductService {

    private final ProductRepository repository;
    public ProductService(ProductRepository repo){

        this.repository = repo;
    }

    @Transactional
    public Product create(String name, String description)
    {
        Product product = new Product(name, description);
        repository.save(product);
        return product;
    }

    @Transactional(readOnly = true)
    public List<Product> findAll()
    {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Product findById(UUID id)
    {
        return repository.findById(id).orElseThrow(()-> new ProductNotFoundException(id));
    }





}
