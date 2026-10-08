package com.weng.licensehub.product.application;

import com.weng.licensehub.product.domain.Product;
import com.weng.licensehub.product.persistence.ProductRepository;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import com.weng.licensehub.user.domain.UserAccount;
import com.weng.licensehub.user.persistence.UserAccountRepository;

public class ProductServiceTest {

        private ProductRepository repository;
        private ProductService service;
        private UserAccountRepository userAccountRepository;

        @BeforeEach
        void setUp() {
                repository = mock(ProductRepository.class);
                userAccountRepository = mock(UserAccountRepository.class);

                service = new ProductService(
                                repository,
                                userAccountRepository);
        }


        @Test
        void createForOwnerAssignsOwnerAndSavesProduct() {

                UserAccount owner = mock(UserAccount.class);

                when(userAccountRepository.findByEmailIgnoreCase(
                                "owner@example.com"))
                                .thenReturn(Optional.of(owner));

                when(repository.save(any(Product.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Product result = service.createForOwner(
                                "owner@example.com",
                                "LicenseHub Desktop",
                                "Desktop product");

                assertSame(owner, result.getOwner());
                assertEquals(
                                "LicenseHub Desktop",
                                result.getName());

                verify(userAccountRepository)
                                .findByEmailIgnoreCase(
                                                "owner@example.com");

                verify(repository).save(result);
        }

        @Test
        void findAllReturnsOnlyOwnersProducts() {

                UUID ownerId = UUID.randomUUID();

                UserAccount owner = mock(UserAccount.class);
                Product product = mock(Product.class);

                when(owner.getId()).thenReturn(ownerId);

                when(userAccountRepository.findByEmailIgnoreCase(
                                "owner@example.com"))
                                .thenReturn(Optional.of(owner));

                when(repository
                                .findAllByOwner_IdOrderByCreatedAtDesc(ownerId))
                                .thenReturn(List.of(product));

                List<Product> result = service.findAll("owner@example.com");

                assertEquals(List.of(product), result);

                verify(repository)
                                .findAllByOwner_IdOrderByCreatedAtDesc(ownerId);
        }

        @Test
        void findByIdThrowsWhenProductIsNotOwned() {

                UUID ownerId = UUID.randomUUID();
                UUID productId = UUID.randomUUID();

                UserAccount owner = mock(UserAccount.class);

                when(owner.getId()).thenReturn(ownerId);

                when(userAccountRepository.findByEmailIgnoreCase(
                                "owner@example.com"))
                                .thenReturn(Optional.of(owner));

                when(repository.findByIdAndOwner_Id(
                                productId,
                                ownerId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ProductNotFoundException.class,
                                () -> service.findById(
                                                productId,
                                                "owner@example.com"));
        }
}
