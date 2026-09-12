package com.gymmind.shared.persistence;

import com.gymmind.support.MySqlIntegrationTest;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class TenantScopedEntityMappingIT extends MySqlIntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void tenantIdIsNotNullable() {
        assertThatThrownBy(() -> {
            entityManager.persist(new ProbeEntity(null, "probe"));
            entityManager.flush();
        })
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    void persistenceGeneratesIdentityVersionAndAuditTimestamps() {
        ProbeEntity probe = new ProbeEntity(41L, "before");

        entityManager.persist(probe);
        entityManager.flush();
        Long probeId = probe.getId();
        entityManager.clear();

        ProbeEntity persistedProbe = entityManager.find(ProbeEntity.class, probeId);

        assertThat(probeId).isPositive();
        assertThat(persistedProbe.getVersion()).isZero();
        assertThat(persistedProbe.getCreatedAt()).isNotNull();
        assertThat(persistedProbe.getUpdatedAt()).isNotNull();
        assertThat(persistedProbe.getUpdatedAt()).isEqualTo(persistedProbe.getCreatedAt());
        assertThat(persistedProbe.getTenantId()).isEqualTo(41L);
    }

    @Test
    void updateAdvancesVersionAndUpdatedAt() throws InterruptedException {
        ProbeEntity probe = new ProbeEntity(42L, "before");
        entityManager.persist(probe);
        entityManager.flush();
        Long probeId = probe.getId();
        entityManager.clear();

        ProbeEntity persistedProbe = entityManager.find(ProbeEntity.class, probeId);
        long initialVersion = persistedProbe.getVersion();
        Instant initialCreatedAt = persistedProbe.getCreatedAt();
        Instant initialUpdatedAt = persistedProbe.getUpdatedAt();

        waitUntilAfter(initialUpdatedAt);
        persistedProbe.rename("after");
        entityManager.flush();
        entityManager.clear();

        ProbeEntity updatedProbe = entityManager.find(ProbeEntity.class, probeId);

        assertThat(updatedProbe.getVersion()).isGreaterThan(initialVersion);
        assertThat(updatedProbe.getCreatedAt()).isEqualTo(initialCreatedAt);
        assertThat(updatedProbe.getUpdatedAt()).isAfter(initialUpdatedAt);
    }

    private static void waitUntilAfter(Instant instant) throws InterruptedException {
        while (!Instant.now().isAfter(instant)) {
            Thread.sleep(1);
        }
    }

    @Entity(name = "TenantScopedEntityMappingProbe")
    @Table(name = "tenant_scoped_entity_mapping_probe")
    static class ProbeEntity extends TenantScopedEntity {

        private String name;

        protected ProbeEntity() {
        }

        ProbeEntity(Long tenantId, String name) {
            super(tenantId);
            this.name = name;
        }

        void rename(String name) {
            this.name = name;
        }
    }
}
