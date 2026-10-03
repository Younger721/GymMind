package com.gymmind.tenancy.application;

import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.shared.api.PageResponse;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.api.response.TenantView;
import com.gymmind.tenancy.application.command.CreateTenantCommand;
import com.gymmind.tenancy.application.command.UpdateTenantCommand;
import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class DefaultPlatformTenantService implements PlatformTenantService {
    private static final int MAX_CODE_ATTEMPTS = 5;

    private final TenantRepository tenantRepository;
    private final TenantProvisioningService provisioningService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserAccountRepository userRepository;

    public DefaultPlatformTenantService(TenantRepository tenantRepository,
                                        TenantProvisioningService provisioningService,
                                        UserAccountRepository userRepository) {
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository");
        this.provisioningService = Objects.requireNonNull(provisioningService, "provisioningService");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
    }

    @Override
    @Transactional
    public TenantView create(CreateTenantCommand command, CurrentActor actor) {
        requirePlatform(actor, "platform:tenant:write");
        Objects.requireNonNull(command, "command");
        String name = required(command.tenantName(), "Tenant name");
        String email = UserAccount.normalizeEmail(required(command.adminEmail(), "Admin email"));
        String password = required(command.adminPassword(), "Admin password");
        String displayName = required(command.adminDisplayName(), "Admin display name");
        String code = generateTenantCode();
        if (email != null && userRepository.findByNormalizedEmail(email).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        TenantProvisioningService.ProvisionedTenant result = provisioningService.provision(
                Tenant.create(code, name),
                new TenantProvisioningService.Administrator(
                        email, null, passwordEncoder.encode(password), displayName));
        return tenantView(result.tenant());
    }

    private String generateTenantCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = "gym-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            if (!tenantRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new BusinessException(ErrorCode.CONFLICT);
    }

    @Override
    @Transactional
    public TenantView update(Long tenantId, UpdateTenantCommand command, CurrentActor actor) {
        requirePlatform(actor, "platform:tenant:write");
        Objects.requireNonNull(command, "command");
        Tenant tenant = tenantRepository.findById(requireId(tenantId))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        tenant.rename(required(command.tenantName(), "Tenant name"));
        return tenantView(tenantRepository.save(tenant));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TenantView> list(CurrentActor actor, Pageable pageable) {
        requirePlatform(actor, "platform:tenant:read");
        Pageable effective = pageable == null ? Pageable.ofSize(20) : pageable;
        Page<Tenant> page = tenantRepository.findAll(effective);
        return new PageResponse<>(page.map(this::tenantView).getContent(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional
    public TenantView activate(Long tenantId, CurrentActor actor) {
        return changeStatus(tenantId, actor, true);
    }

    @Override
    @Transactional
    public TenantView disable(Long tenantId, CurrentActor actor) {
        return changeStatus(tenantId, actor, false);
    }

    private TenantView changeStatus(Long tenantId, CurrentActor actor, boolean activate) {
        requirePlatform(actor, "platform:tenant:write");
        Tenant tenant = tenantRepository.findById(requireId(tenantId))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (activate) {
            tenant.activate();
        } else {
            tenant.disable();
            userRepository.findAllByTenantId(tenant.getId(), Pageable.unpaged()).forEach(user -> {
                user.disable();
                userRepository.save(user);
            });
        }
        return tenantView(tenantRepository.save(tenant));
    }

    private TenantView tenantView(Tenant tenant) {
        return TenantView.from(tenant, userRepository.countByTenantId(tenant.getId()));
    }

    private static void requirePlatform(CurrentActor actor, String permission) {
        if (actor == null || !actor.isPlatformAdmin() || !actor.hasPermission(permission)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static Long requireId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return id;
    }

    private static String required(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, label + " must not be blank");
        }
        return value.trim();
    }

}
