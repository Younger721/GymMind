package com.gymmind.agent.infrastructure.persistence;

import com.gymmind.agent.domain.model.AgentStatus;
import com.gymmind.agent.domain.model.TenantAgent;
import com.gymmind.agent.domain.repository.TenantAgentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class JpaTenantAgentRepository implements TenantAgentRepository {

    private final SpringDataTenantAgentRepository delegate;

    JpaTenantAgentRepository(SpringDataTenantAgentRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public TenantAgent save(TenantAgent agent) {
        return delegate.save(agent);
    }

    @Override
    public Optional<TenantAgent> findByTenantIdAndId(Long tenantId, Long id) {
        return delegate.findByTenantIdAndId(tenantId, id);
    }

    @Override
    public List<TenantAgent> findAllByTenantId(Long tenantId) {
        return delegate.findAllByTenantId(tenantId);
    }

    @Override
    public List<TenantAgent> findAllByTenantIdAndStatus(Long tenantId, AgentStatus status) {
        return delegate.findAllByTenantIdAndStatus(tenantId, status);
    }

    @Override
    public long countByTenantId(Long tenantId) {
        return delegate.countByTenantId(tenantId);
    }

    @Override
    public Optional<TenantAgent> findById(Long id) {
        return delegate.findById(id);
    }

    @Override
    public List<TenantAgent> findAllAccessible() {
        return delegate.findTop200ByOrderByIdDesc();
    }
}
