package com.weng.licensehub.license.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weng.licensehub.license.domain.License;

public interface LicenseRepository extends JpaRepository<License, UUID> {

    List<License> findAllByProduct_IdOrderByCreatedAtDesc(UUID productId);
}
