package com.gymmind.knowledge.bootstrap;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import com.gymmind.knowledge.application.KnowledgeDocumentRepository;
import com.gymmind.knowledge.application.KnowledgeDocumentUseCase;
import com.gymmind.knowledge.application.UploadKnowledgeDocumentCommand;
import com.gymmind.knowledge.domain.model.DocumentVisibility;
import com.gymmind.knowledge.domain.model.KnowledgeCategory;
import com.gymmind.knowledge.domain.model.KnowledgeType;
import com.gymmind.platform.bootstrap.DatabaseInitializationLock;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.model.TenantStatus;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 开发环境注入 8 类健身 RAG 演示文档，便于毕设/演示问答闭环。
 */
@Component
@Profile("dev")
@Order(360)
public class FitnessKnowledgeDemoInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FitnessKnowledgeDemoInitializer.class);
    private static final Set<String> GYM_KNOWLEDGE_PERMS = Set.of("knowledge:read", "knowledge:write");

    private record DemoDoc(KnowledgeCategory category, String resourcePath, String fileName) {
    }

    private static final List<DemoDoc> DOCS = List.of(
            new DemoDoc(KnowledgeCategory.ACTION_STANDARD, "demo-knowledge/action-standard.md", "深蹲动作规范.md"),
            new DemoDoc(KnowledgeCategory.TRAINING_BASICS, "demo-knowledge/training-basics.md", "训练基础知识.md"),
            new DemoDoc(KnowledgeCategory.EQUIPMENT_GUIDE, "demo-knowledge/equipment-guide.md", "史密斯机使用说明.md"),
            new DemoDoc(KnowledgeCategory.COURSE_INTRO, "demo-knowledge/course-intro.md", "团体课程介绍.md"),
            new DemoDoc(KnowledgeCategory.NUTRITION_BASICS, "demo-knowledge/nutrition-basics.md", "饮食基础知识.md"),
            new DemoDoc(KnowledgeCategory.GYM_RULES, "demo-knowledge/gym-rules.md", "健身房规章制度.md"),
            new DemoDoc(KnowledgeCategory.FAQ, "demo-knowledge/faq.md", "常见健身问题.md"));

    private final DatabaseInitializationLock initializationLock;
    private final KnowledgeDocumentUseCase knowledge;
    private final KnowledgeDocumentRepository documents;
    private final TenantRepository tenants;
    private final UserAccountRepository users;
    private final UserRoleRepository userRoles;
    private final TransactionTemplate transactionTemplate;

    public FitnessKnowledgeDemoInitializer(
            DatabaseInitializationLock initializationLock,
            KnowledgeDocumentUseCase knowledge,
            KnowledgeDocumentRepository documents,
            TenantRepository tenants,
            UserAccountRepository users,
            UserRoleRepository userRoles,
            TransactionTemplate transactionTemplate) {
        this.initializationLock = initializationLock;
        this.knowledge = knowledge;
        this.documents = documents;
        this.tenants = tenants;
        this.users = users;
        this.userRoles = userRoles;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            initializationLock.execute("gymmind:fitness-knowledge-demo",
                    () -> transactionTemplate.executeWithoutResult(status -> seedForActiveTenants()));
        } catch (Exception ex) {
            log.error("健身知识演示文档导入失败，可检查 MinIO/索引后重启重试", ex);
        }
    }

    void seedForActiveTenants() {
        List<Long> tenantIds = tenants.findAll(PageRequest.of(0, 10, Sort.by("id"))).stream()
                .filter(t -> t.getStatus() == TenantStatus.ACTIVE)
                .map(Tenant::getId)
                .toList();
        for (Long tenantId : tenantIds) {
            Long gymAdminId = findGymAdminId(tenantId);
            if (gymAdminId == null) {
                continue;
            }
            CurrentActor actor = new CurrentActor(gymAdminId, tenantId,
                    Set.of(RoleCode.GYM_ADMIN), GYM_KNOWLEDGE_PERMS, 0L, "fitness-knowledge-demo");
            for (DemoDoc doc : DOCS) {
                boolean alreadySeeded = documents.findByTenantId(tenantId).stream()
                        .anyMatch(d -> doc.category().code().equals(d.category())
                                && doc.fileName().equals(d.fileName()));
                if (alreadySeeded) {
                    continue;
                }
                byte[] content = readResource(doc.resourcePath());
                // 首次上传不传 knowledgeKey（由文档服务在替换版本时再维护 key）
                knowledge.upload(actor, new UploadKnowledgeDocumentCommand(
                        doc.fileName(),
                        "text/markdown",
                        content,
                        DocumentVisibility.TENANT,
                        doc.category().code(),
                        List.of(RoleCode.MEMBER, RoleCode.COACH, RoleCode.GYM_ADMIN),
                        null,
                        KnowledgeType.GENERAL_NOTICE,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));
                log.info("已导入健身演示知识 tenantId={} category={}", tenantId, doc.category());
            }
        }
    }

    private static byte[] readResource(String path) {
        try {
            return new ClassPathResource(path).getContentAsByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("读取演示知识失败: " + path, ex);
        }
    }

    private Long findGymAdminId(Long tenantId) {
        for (Long userId : userRoles.findUserIdsByRoleCodes(Set.of(RoleCode.GYM_ADMIN))) {
            Optional<UserAccount> account = users.findById(userId);
            if (account.isPresent() && tenantId.equals(account.get().getTenantId())) {
                return userId;
            }
        }
        return null;
    }
}
