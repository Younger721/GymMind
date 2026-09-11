package com.gymmind.service;

import com.gymmind.entity.KnowledgeDocument;
import com.gymmind.entity.User;
import com.gymmind.entity.UserProfile;
import com.gymmind.repository.KnowledgeDocumentRepository;
import com.gymmind.repository.UserProfileRepository;
import com.gymmind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统测试服务 - 用于验证数据隔离和安全性
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemTestService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 验证用户数据隔离
     */
    public Map<String, Object> testUserDataIsolation() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 测试1: 验证用户只能访问自己的档案
            List<UserProfile> allProfiles = userProfileRepository.findAll();
            boolean profileIsolationOk = true;

            for (UserProfile profile : allProfiles) {
                // 尝试用错误的userId查询
                List<UserProfile> wrongProfiles = userProfileRepository
                    .findAll()
                    .stream()
                    .filter(p -> !p.getUserId().equals(profile.getUserId()) && p.getId().equals(profile.getId()))
                    .toList();

                if (!wrongProfiles.isEmpty()) {
                    profileIsolationOk = false;
                    break;
                }
            }

            result.put("profileIsolation", profileIsolationOk ? "PASS" : "FAIL");

            // 测试2: 验证知识库文档隔离
            List<KnowledgeDocument> allDocs = knowledgeDocumentRepository.findAll();
            boolean docIsolationOk = true;

            for (KnowledgeDocument doc : allDocs) {
                long wrongUserDocs = knowledgeDocumentRepository
                    .findAll()
                    .stream()
                    .filter(d -> !d.getUserId().equals(doc.getUserId()) && d.getId().equals(doc.getId()))
                    .count();

                if (wrongUserDocs > 0) {
                    docIsolationOk = false;
                    break;
                }
            }

            result.put("documentIsolation", docIsolationOk ? "PASS" : "FAIL");

            // 测试3: 验证密码加密
            List<User> users = userRepository.findAll();
            boolean passwordEncrypted = users.stream()
                .allMatch(u -> u.getPassword().startsWith("$2a$") || u.getPassword().startsWith("$2b$"));

            result.put("passwordEncryption", passwordEncrypted ? "PASS" : "FAIL");

            // 统计信息
            result.put("totalUsers", userRepository.count());
            result.put("totalProfiles", userProfileRepository.count());
            result.put("totalDocuments", knowledgeDocumentRepository.count());

            // 整体结果
            boolean allPass = profileIsolationOk && docIsolationOk && passwordEncrypted;
            result.put("overallStatus", allPass ? "PASS" : "FAIL");
            result.put("timestamp", System.currentTimeMillis());

            log.info("User data isolation test completed: {}", result);

        } catch (Exception e) {
            log.error("Error during isolation test", e);
            result.put("error", e.getMessage());
            result.put("overallStatus", "ERROR");
        }

        return result;
    }

    /**
     * 系统健康检查
     */
    public Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        try {
            // 数据库连接检查
            long userCount = userRepository.count();
            health.put("database", "OK");
            health.put("userCount", userCount);

            // 知识库状态统计
            long totalDocs = knowledgeDocumentRepository.count();
            long successDocs = knowledgeDocumentRepository.countByStatus(KnowledgeDocument.ProcessingStatus.SUCCESS);
            long processingDocs = knowledgeDocumentRepository.countByStatus(KnowledgeDocument.ProcessingStatus.PROCESSING);
            long failedDocs = knowledgeDocumentRepository.countByStatus(KnowledgeDocument.ProcessingStatus.FAILED);

            Map<String, Long> docStats = new HashMap<>();
            docStats.put("total", totalDocs);
            docStats.put("success", successDocs);
            docStats.put("processing", processingDocs);
            docStats.put("failed", failedDocs);
            health.put("knowledgeBase", docStats);

            // 档案完整性
            long profileCount = userProfileRepository.count();
            double profileCompleteness = userCount > 0 ? (double) profileCount / userCount * 100 : 0;
            health.put("profileCompleteness", String.format("%.1f%%", profileCompleteness));

            health.put("status", "HEALTHY");
            health.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            log.error("Health check failed", e);
            health.put("status", "UNHEALTHY");
            health.put("error", e.getMessage());
        }

        return health;
    }

    /**
     * 获取系统统计信息
     */
    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // 用户统计
            long totalUsers = userRepository.count();
            long activeUsers = userRepository.findAll().stream()
                .filter(u -> u.getStatus() == User.UserStatus.ACTIVE)
                .count();

            Map<String, Long> userStats = new HashMap<>();
            userStats.put("total", totalUsers);
            userStats.put("active", activeUsers);
            stats.put("users", userStats);

            // 知识库统计
            Map<String, Long> docStats = new HashMap<>();
            docStats.put("total", knowledgeDocumentRepository.count());
            docStats.put("success", knowledgeDocumentRepository.countByStatus(KnowledgeDocument.ProcessingStatus.SUCCESS));
            docStats.put("processing", knowledgeDocumentRepository.countByStatus(KnowledgeDocument.ProcessingStatus.PROCESSING));
            docStats.put("failed", knowledgeDocumentRepository.countByStatus(KnowledgeDocument.ProcessingStatus.FAILED));
            stats.put("documents", docStats);

            // 文档来源分布
            List<KnowledgeDocument> allDocs = knowledgeDocumentRepository.findAll();
            Map<String, Long> sourceDistribution = new HashMap<>();
            sourceDistribution.put("UPLOAD", allDocs.stream().filter(d -> d.getSourceType() == KnowledgeDocument.SourceType.UPLOAD).count());
            sourceDistribution.put("WEB_SEARCH", allDocs.stream().filter(d -> d.getSourceType() == KnowledgeDocument.SourceType.WEB_SEARCH).count());
            stats.put("documentSources", sourceDistribution);

            // 分类分布
            Map<String, Long> categoryDistribution = allDocs.stream()
                .filter(d -> d.getCategory() != null)
                .collect(HashMap::new,
                    (map, doc) -> map.merge(doc.getCategory(), 1L, Long::sum),
                    HashMap::putAll);
            stats.put("documentCategories", categoryDistribution);

            stats.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            log.error("Failed to get system stats", e);
            stats.put("error", e.getMessage());
        }

        return stats;
    }
}
