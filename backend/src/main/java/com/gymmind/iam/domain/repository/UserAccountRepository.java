package com.gymmind.iam.domain.repository;

import com.gymmind.iam.domain.model.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface UserAccountRepository {
    UserAccount save(UserAccount user);
    Optional<UserAccount> findById(Long id);
    Optional<UserAccount> findByNormalizedEmail(String normalizedEmail);
    Optional<UserAccount> findByNormalizedUsername(String normalizedUsername);
    Optional<UserAccount> findByTenantIdAndId(Long tenantId, Long id);
    Page<UserAccount> findAllByTenantId(Long tenantId, Pageable pageable);
    Page<UserAccount> findAll(Pageable pageable);
    /** 当前租户用户 + 全部平台管理员（tenantId 为空） */
    Page<UserAccount> findByTenantIdOrTenantIdIsNullOrderByIdAsc(Long tenantId, Pageable pageable);
    List<UserAccount> findAll();
}
