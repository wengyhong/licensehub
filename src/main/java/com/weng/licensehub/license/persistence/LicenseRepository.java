package com.weng.licensehub.license.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weng.licensehub.license.domain.License;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface LicenseRepository extends JpaRepository<License, UUID> {

    List<License> findAllByProduct_IdOrderByCreatedAtDesc(UUID productId);
    Optional<License> findByKeyId(String keyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
        select license
        from License license
        where license.id = :licenseId
        """)
Optional<License> findByIdForUpdate(
        @Param("licenseId") UUID licenseId);
}
