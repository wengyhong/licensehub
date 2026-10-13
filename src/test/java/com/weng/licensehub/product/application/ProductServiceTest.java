package com.weng.licensehub.product.application;

import com.weng.licensehub.license.persistence.LicenseRepository;
import com.weng.licensehub.product.domain.Product;
import com.weng.licensehub.product.persistence.ProductRepository;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public class ProductServiceTest {

        private ProductRepository repository;
        private ProductService service;
        private UserAccountRepository userAccountRepository;
        private LicenseRepository licenseRepository;

        @BeforeEach
        void setUp() {
                repository = mock(ProductRepository.class);
                userAccountRepository = mock(UserAccountRepository.class);
                licenseRepository = mock(LicenseRepository.class);
                service = new ProductService(
                                repository,
                                userAccountRepository,
                                licenseRepository);
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

                Pageable pageable = PageRequest.of(0, 20);

                Page<Product> repositoryResult = new PageImpl<>(
                                List.of(product),
                                pageable,
                                1);

                when(owner.getId()).thenReturn(ownerId);

                when(userAccountRepository.findByEmailIgnoreCase(
                                "owner@example.com"))
                                .thenReturn(Optional.of(owner));

                when(repository.findAllByOwner_Id(
                                ownerId,
                                pageable))
                                .thenReturn(repositoryResult);

                Page<Product> result = service.findAll(
                                "owner@example.com",
                                pageable);

                assertThat(result.getContent())
                                .containsExactly(product);

                assertThat(result.getTotalElements())
                                .isEqualTo(1);

                verify(repository).findAllByOwner_Id(
                                ownerId,
                                pageable);
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

        @Test
        void updatesProductForOwner() {
                UUID productId = UUID.randomUUID();
                UUID ownerId = UUID.randomUUID();
                String ownerEmail = "owner@example.com";

                UserAccount owner = mock(UserAccount.class);
                Product product = mock(Product.class);

                when(userAccountRepository.findByEmailIgnoreCase(ownerEmail))
                                .thenReturn(Optional.of(owner));

                when(owner.getId()).thenReturn(ownerId);

                when(repository.findByIdAndOwner_Id(
                                productId,
                                ownerId))
                                .thenReturn(Optional.of(product));

                Product result = service.updateForOwner(
                                productId,
                                ownerEmail,
                                "Updated Product",
                                "Updated description");

                assertThat(result).isSameAs(product);

                verify(product).updateDetails(
                                "Updated Product",
                                "Updated description");
        }

        @Test
        void updateThrowsWhenProductIsNotOwned() {
                UUID productId = UUID.randomUUID();
                UUID ownerId = UUID.randomUUID();
                String ownerEmail = "owner@example.com";

                UserAccount owner = mock(UserAccount.class);

                when(userAccountRepository.findByEmailIgnoreCase(ownerEmail))
                                .thenReturn(Optional.of(owner));

                when(owner.getId()).thenReturn(ownerId);

                when(repository.findByIdAndOwner_Id(
                                productId,
                                ownerId))
                                .thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.updateForOwner(
                                productId,
                                ownerEmail,
                                "Updated Product",
                                null))
                                .isInstanceOf(ProductNotFoundException.class);
        }

        @Test
        void deletesOwnedProductWithoutLicenses() {
                UUID productId = UUID.randomUUID();
                UUID ownerId = UUID.randomUUID();
                String ownerEmail = "owner@example.com";

                UserAccount owner = mock(UserAccount.class);
                Product product = mock(Product.class);

                when(userAccountRepository.findByEmailIgnoreCase(ownerEmail))
                                .thenReturn(Optional.of(owner));

                when(owner.getId()).thenReturn(ownerId);

                when(repository.findByIdAndOwner_Id(
                                productId,
                                ownerId))
                                .thenReturn(Optional.of(product));

                when(licenseRepository.existsByProduct_Id(productId))
                                .thenReturn(false);

                service.deleteForOwner(productId, ownerEmail);

                verify(repository).delete(product);
        }

        @Test
        void deleteThrowsWhenProductHasLicenses() {
                UUID productId = UUID.randomUUID();
                UUID ownerId = UUID.randomUUID();
                String ownerEmail = "owner@example.com";

                UserAccount owner = mock(UserAccount.class);
                Product product = mock(Product.class);

                when(userAccountRepository.findByEmailIgnoreCase(ownerEmail))
                                .thenReturn(Optional.of(owner));

                when(owner.getId()).thenReturn(ownerId);

                when(repository.findByIdAndOwner_Id(
                                productId,
                                ownerId))
                                .thenReturn(Optional.of(product));

                when(licenseRepository.existsByProduct_Id(productId))
                                .thenReturn(true);

                assertThatThrownBy(
                                () -> service.deleteForOwner(
                                                productId,
                                                ownerEmail))
                                .isInstanceOf(
                                                ProductHasLicensesException.class);

                verify(repository, never()).delete(any());
        }
}
