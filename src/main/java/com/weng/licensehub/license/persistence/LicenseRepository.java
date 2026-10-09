package com.weng.licensehub.license.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.weng.licensehub.license.domain.License;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LicenseRepository extends JpaRepository<License, UUID> {

        Page<License> findAllByProduct_Id(UUID productId, Pageable pageable);

        Optional<License> findByKeyId(String keyId);

        Optional<License> findByIdAndProduct_Owner_Id(UUID productId, UUID ownerId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
                        select license
                        from License license
                        where license.id = :licenseId
                        """)
        Optional<License> findByIdForUpdate(
                        @Param("licenseId") UUID licenseId);

        boolean existsByProduct_Id(UUID productId);
}
