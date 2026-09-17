package com.gymmind.agent.application;

import com.gymmind.agent.domain.model.TenantAgent;
import com.gymmind.agent.domain.repository.TenantAgentRepository;
import com.gymmind.tenancy.application.port.TenantProvisioningContributor;
import com.gymmind.tenancy.domain.model.Tenant;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(200)
public class AgentProvisioningContributor implements TenantProvisioningContributor {

    private final TenantAgentRepository agents;

    public AgentProvisioningContributor(TenantAgentRepository agents) {
        this.agents = agents;
    }

    @Override
    public void contribute(Tenant tenant) {
        if (agents.countByTenantId(tenant.getId()) > 0) {
            return;
        }
        agents.save(TenantAgent.create(
                tenant.getId(),
                "门店智能助手",
                "默认健身房 AI 助手，基于本店知识库回答会员问题。",
                "你是本健身房的智能助手，请基于参考资料给出专业、安全、简洁的健身建议。不要编造不存在的服务或价格。",
                true,
                "knowledge_search",
                null));
    }
}
