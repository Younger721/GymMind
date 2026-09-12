package com.gymmind.tenancy.infrastructure.persistence;

import com.gymmind.tenancy.domain.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface SpringDataTenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByCode(String code);

    boolean existsByCode(String code);
}
