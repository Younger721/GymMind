package com.gymmind.agent.domain.repository;

import com.gymmind.agent.domain.model.AgentStatus;
import com.gymmind.agent.domain.model.TenantAgent;

import java.util.List;
import java.util.Optional;

public interface TenantAgentRepository {
    TenantAgent save(TenantAgent agent);

    Optional<TenantAgent> findByTenantIdAndId(Long tenantId, Long id);

    List<TenantAgent> findAllByTenantId(Long tenantId);

    List<TenantAgent> findAllByTenantIdAndStatus(Long tenantId, AgentStatus status);

    long countByTenantId(Long tenantId);
}
