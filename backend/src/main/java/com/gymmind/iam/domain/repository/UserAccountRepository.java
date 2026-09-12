package com.gymmind.iam.domain.repository;

import com.gymmind.iam.domain.model.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface UserAccountRepository {
    UserAccount save(UserAccount user);
    Optional<UserAccount> findById(Long id);
    Optional<UserAccount> findByNormalizedEmail(String normalizedEmail);
    Optional<UserAccount> findByTenantIdAndId(Long tenantId, Long id);
    Page<UserAccount> findAllByTenantId(Long tenantId, Pageable pageable);
}
