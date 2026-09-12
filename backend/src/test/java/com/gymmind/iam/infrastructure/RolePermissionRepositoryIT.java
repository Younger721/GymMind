package com.gymmind.iam.infrastructure;

import com.gymmind.iam.domain.model.Permission;
import com.gymmind.iam.domain.model.Role;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.RolePermission;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.model.UserRole;
import com.gymmind.iam.domain.repository.PermissionRepository;
import com.gymmind.iam.domain.repository.RoleRepository;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.support.MySqlIntegrationTest;
import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;
import jakarta.persistence.metamodel.Attribute;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class RolePermissionRepositoryIT extends MySqlIntegrationTest {

    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private UserAccountRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @PersistenceContext private EntityManager entityManager;

    @Test
    void persistsRolePermissionAndUserRoleAssociations() {
        Role role = roleRepository.findByCode(RoleCode.COACH).orElseThrow();
        Permission permission = permissionRepository.save(Permission.of("workout:read", "Read workouts"));
        Long tenantId = tenantRepository.save(Tenant.create("role-gym", "Role Gym")).getId();
        UserAccount user = userRepository.save(UserAccount.tenantUser(tenantId, "coach@example.com", "$hash", "Coach"));
        entityManager.flush();

        entityManager.persist(RolePermission.link(role, permission));
        entityManager.persist(UserRole.assign(user, role));
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(RolePermission.class, new RolePermission.Key(role.getId(), permission.getId())))
                .isNotNull();
        assertThat(entityManager.find(UserRole.class, new UserRole.Key(user.getId(), role.getId())))
                .isNotNull();
    }

    @Test
    void userRoleIsTheOnlyPersistedRoleSourceForUserAccount() {
        assertThat(entityManager.getMetamodel().entity(UserAccount.class).getAttributes())
                .extracting(Attribute::getName)
                .doesNotContain("roleCode");
    }

    @Test
    void associationPairsAreUnique() {
        Role role = roleRepository.findByCode(RoleCode.MEMBER).orElseThrow();
        Permission permission = permissionRepository.save(Permission.of("profile:read", "Read profile"));
        entityManager.flush();

        entityManager.persist(RolePermission.link(role, permission));
        entityManager.flush();
        entityManager.clear();

        Role managedRole = entityManager.find(Role.class, role.getId());
        Permission managedPermission = entityManager.find(Permission.class, permission.getId());
        entityManager.persist(RolePermission.link(managedRole, managedPermission));
        assertThatThrownBy(() -> entityManager.flush())
                .isInstanceOfAny(DataIntegrityViolationException.class, ConstraintViolationException.class);
    }
}
