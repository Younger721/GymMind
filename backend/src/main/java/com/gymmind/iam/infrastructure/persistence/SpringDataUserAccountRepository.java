package com.gymmind.iam.infrastructure.persistence;

import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.RoleStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

interface SpringDataUserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByNormalizedEmail(String normalizedEmail);
    Optional<UserAccount> findByNormalizedUsername(String normalizedUsername);
    Optional<UserAccount> findByTenantIdAndId(Long tenantId, Long id);
    Page<UserAccount> findAllByTenantId(Long tenantId, Pageable pageable);
    @Query(value = """
            select user
            from UserAccount user
            where user.tenantId = :tenantId
              and exists (
                  select userRole.id
                  from UserRole userRole
                  where userRole.user.id = user.id
                    and userRole.role.code = :roleCode
                    and userRole.role.status = :activeRoleStatus
              )
              and (
                  select count(distinct otherRole.id)
                  from UserRole otherUserRole
                  join otherUserRole.role otherRole
                  where otherUserRole.user.id = user.id
                    and otherRole.status = :activeRoleStatus
              ) = 1
            """,
            countQuery = """
                    select count(user)
                    from UserAccount user
                    where user.tenantId = :tenantId
                      and exists (
                          select userRole.id
                          from UserRole userRole
                          where userRole.user.id = user.id
                            and userRole.role.code = :roleCode
                            and userRole.role.status = :activeRoleStatus
                      )
                      and (
                          select count(distinct otherRole.id)
                          from UserRole otherUserRole
                          join otherUserRole.role otherRole
                          where otherUserRole.user.id = user.id
                            and otherRole.status = :activeRoleStatus
                      ) = 1
                    """)
    Page<UserAccount> findAllByTenantIdAndRoleCode(@Param("tenantId") Long tenantId,
                                                   @Param("roleCode") RoleCode roleCode,
                                                   @Param("activeRoleStatus") RoleStatus activeRoleStatus,
                                                   Pageable pageable);
    long countByTenantId(Long tenantId);
    Page<UserAccount> findByTenantIdOrTenantIdIsNullOrderByIdAsc(Long tenantId, Pageable pageable);
}
