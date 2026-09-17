package com.gymmind.ai.harness.application;

import com.gymmind.ai.harness.HarnessPluginList;
import com.gymmind.ai.harness.config.AiHarnessProperties;
import com.gymmind.ai.harness.domain.TenantHarnessConfig;
import com.gymmind.ai.harness.domain.TenantHarnessConfigRepository;
import com.gymmind.tenancy.application.port.TenantProvisioningContributor;
import com.gymmind.tenancy.domain.model.Tenant;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 新租户开通时写入默认 Harness 插件配置 */
@Component
@Order(150)
public class HarnessProvisioningContributor implements TenantProvisioningContributor {

    private final TenantHarnessConfigRepository configs;
    private final AiHarnessProperties properties;

    public HarnessProvisioningContributor(TenantHarnessConfigRepository configs, AiHarnessProperties properties) {
        this.configs = configs;
        this.properties = properties;
    }

    @Override
    public void contribute(Tenant tenant) {
        if (configs.findByTenantId(tenant.getId()).isPresent()) {
            return;
        }
        configs.save(TenantHarnessConfig.defaults(
                tenant.getId(),
                HarnessPluginList.join(properties.getTenantDefaultPlugins()),
                properties.getDefaultSystemPrompt()));
    }
}
