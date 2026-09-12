package com.gymmind.iam.infrastructure.persistence;

import com.gymmind.iam.domain.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

interface SpringDataUserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByNormalizedEmail(String normalizedEmail);
    Optional<UserAccount> findByTenantIdAndId(Long tenantId, Long id);
    Page<UserAccount> findAllByTenantId(Long tenantId, Pageable pageable);
}
