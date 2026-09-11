# GymMind 基础、租户与 IAM Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重建 GymMind Spring Boot 工程并交付可启动的基础设施、精确受控的 `gymmind` 数据库重置、独立健身房租户、IAM、JWT、RBAC、用户邀请和跨租户隔离。

**Architecture:** 基础能力放在 `shared`，Tenant 与 IAM 保持独立纵向模块，业务请求只能从验证后的 JWT 得到 `CurrentActor` 和 tenantId。Repository 对租户业务只暴露显式 tenantId 查询；Redis 保存刷新会话与访问令牌撤销，MySQL 保存账户、角色、权限和 tokenVersion。

**Tech Stack:** Java 17、Spring Boot 3.4.12、Maven Wrapper、Spring MVC、Validation、Actuator、Spring Security、Spring Data JPA、MySQL 8、Redis 7、JJWT 0.12.6、springdoc-openapi 2.8.14、JUnit 5、AssertJ、Mockito、ArchUnit、Testcontainers。

## Global Constraints

- 所有 API 使用 `/api/v1`；不保留旧后端接口兼容层。
- 数据库名只能是精确小写 `gymmind`，不使用 Flyway；dev=`update`、test=`create-drop`、prod=`validate`。
- 一个租户是一家独立健身房；租户之间无关联、无共享、无总部或门店层级。
- 平台管理员 tenantId 为 null，只能管理租户状态和脱敏汇总，不能调用租户明细 API。
- 租户业务请求 DTO 禁止包含 `tenantId`；范围只能来自 `CurrentActor`。
- JWT 至少包含 userId、tenantId、roles、tokenVersion、jti、issuer、类型和过期时间。
- 密码使用 BCrypt；刷新令牌只保存摘要；Redis 不可用时认证撤销检查 fail closed。
- 真实密钥、数据库密码和 API Key 不能进入代码、测试、日志或 Git。
- `*Test` 由 Surefire 运行且不依赖 Docker；`*IT` 由 `integration` profile 的 Failsafe 运行。
- 每一任务先运行新增测试看到正确失败，再实现并运行到绿色；绿色测试与最小实现放在同一原子提交。
- 从 `backend/` 运行命令；本机执行前设置 `JAVA_HOME=D:\java\jdk-17.0.14`。

---

## File Structure

```text
backend/src/main/java/com/gymmind/
├── GymMindApplication.java
├── shared/
│   ├── api/{ApiResponse,PageResponse,TraceIdFilter}.java
│   ├── error/{ErrorCode,BusinessException,GlobalExceptionHandler}.java
│   ├── config/{JpaAuditingConfig,OpenApiConfig,SecurityProperties}.java
│   ├── persistence/{BaseEntity,AuditableEntity,TenantScopedEntity}.java
│   └── security/{CurrentActor,CurrentActorProvider,GymMindPrincipal,TenantAccessGuard}.java
├── platform/{bootstrap,database}/
├── tenancy/{api,application,domain,infrastructure}/
└── iam/{api,application,domain,infrastructure}/
```

关键契约固定如下，后续业务模块直接消费，不重复定义：

```java
public record CurrentActor(
        Long userId,
        Long tenantId,
        Set<String> roles,
        Set<String> permissions,
        long tokenVersion,
        String tokenId) {
    public boolean isPlatformAdmin();
    public boolean hasPermission(String permission);
}

public interface CurrentActorProvider {
    Optional<CurrentActor> current();
    CurrentActor requireCurrent();
}

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        String traceId,
        Instant timestamp) {
    public static <T> ApiResponse<T> success(T data);
    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String traceId);
}
```

### Task 1: 重建 Maven 与包结构基线

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/java/com/gymmind/GymMindApplication.java`
- Delete: `backend/src/main/java/com/gymmind/{common,config,controller,dto,entity,repository,security,service}/**`
- Create: `backend/mvnw`
- Create: `backend/mvnw.cmd`
- Create: `backend/.mvn/wrapper/maven-wrapper.properties`
- Test: `backend/src/test/java/com/gymmind/architecture/PackageLayoutTest.java`

**Interfaces:**
- Produces: Java 17 / Spring Boot 3.4.12 构建、Surefire `*Test`、Failsafe `*IT` 和 `integration` profile。
- Consumes: 无。

- [ ] **Step 1: 写旧结构失败测试**

```java
@AnalyzeClasses(packages = "com.gymmind")
class PackageLayoutTest {
    @ArchTest
    static final ArchRule noLegacyTopLevelPackages = noClasses()
            .should().resideInAnyPackage(
                    "com.gymmind.controller..", "com.gymmind.service..",
                    "com.gymmind.entity..", "com.gymmind.repository..",
                    "com.gymmind.security..", "com.gymmind.common..",
                    "com.gymmind.dto..");
}
```

- [ ] **Step 2: 在 Java 17 下运行并确认失败原因是旧包仍存在**

```powershell
$env:JAVA_HOME='D:\java\jdk-17.0.14'
mvn -f backend/pom.xml -Dtest=PackageLayoutTest test
```

- [ ] **Step 3: 替换 Maven 基线并删除旧实现**

`pom.xml` 固定引入 `web`、`validation`、`actuator`、`security`、`data-jpa`、`data-redis`、MySQL Connector/J、JJWT 0.12.6、springdoc 2.8.14、`starter-test`、`security-test`、ArchUnit、Testcontainers MySQL；Spring Boot parent 使用 `3.4.12`。暂不引入 ES、Milvus、MinIO 或 Spring AI，避免第一阶段外部连接影响启动。

- [ ] **Step 4: 生成仅脚本型 Maven Wrapper 并验证包结构**

```powershell
mvn -f backend/pom.xml wrapper:wrapper -Dtype=only-script
backend\mvnw.cmd -f backend/pom.xml -Dtest=PackageLayoutTest test
```

- [ ] **Step 5: 提交**

```powershell
git add backend/pom.xml backend/mvnw backend/mvnw.cmd backend/.mvn/wrapper/maven-wrapper.properties backend/src/main/java backend/src/test/java/com/gymmind/architecture/PackageLayoutTest.java
git commit -m "refactor: 重建Spring Boot后端工程基线"
```

### Task 2: 建立分环境配置与启动安全策略

**Files:**
- Create: `backend/src/main/java/com/gymmind/shared/config/SecurityProperties.java`
- Create: `backend/src/main/java/com/gymmind/shared/config/JpaAuditingConfig.java`
- Modify: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/application-dev.yml`
- Create: `backend/src/main/resources/application-test.yml`
- Create: `backend/src/main/resources/application-prod.yml`
- Create: `backend/src/test/resources/application-test.yml`
- Create: `backend/.env.example`
- Test: `backend/src/test/java/com/gymmind/shared/config/ConfigurationPolicyTest.java`
- Test: `backend/src/test/java/com/gymmind/shared/config/SecurityPropertiesTest.java`

**Interfaces:**
- Produces: `SecurityProperties.Jwt(issuer, secret, accessTtl, refreshTtl)`；启动时强制 JWT secret 至少 32 字节。
- Consumes: Task 1 Spring Boot 基线。

- [ ] **Step 1: 写配置策略测试**

```java
class SecurityPropertiesTest {
    @Test
    void rejectsJwtSecretShorterThan256Bits() {
        var jwt = new SecurityProperties.Jwt("gymmind", "too-short", Duration.ofMinutes(15), Duration.ofDays(7));
        assertThatThrownBy(jwt::validate).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256");
    }
}
```

`ConfigurationPolicyTest` 解析四个 YAML，断言数据库 URL 的 schema 为 `gymmind`、dev `update`、test `create-drop`、prod `validate`，并扫描资源确认不存在 `sk-` 或非占位密码。

- [ ] **Step 2: 运行并确认缺少配置类/YAML 导致失败**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=ConfigurationPolicyTest,SecurityPropertiesTest test
```

- [ ] **Step 3: 实现配置**

```yaml
# application.yml
spring:
  application:
    name: gymmind-backend
  profiles:
    default: dev
server:
  port: ${SERVER_PORT:8080}
management:
  endpoints:
    web:
      exposure:
        include: health,info
gymmind:
  security:
    jwt:
      issuer: GymMind
      secret: ${JWT_SECRET:}
      access-ttl: PT15M
      refresh-ttl: P7D
```

dev 数据源从 `MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_USER`、`MYSQL_PASSWORD` 注入；test 配置只供容器动态覆盖；prod 缺失环境变量必须启动失败。

- [ ] **Step 4: 验证通过并提交**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=ConfigurationPolicyTest,SecurityPropertiesTest test
git add backend/src/main/resources backend/src/test/resources backend/src/main/java/com/gymmind/shared/config backend/src/test/java/com/gymmind/shared/config backend/.env.example
git commit -m "chore: 建立分环境配置与启动安全策略"
```

### Task 3: 统一 API 响应、异常与 traceId

**Files:**
- Create: `backend/src/main/java/com/gymmind/shared/api/ApiResponse.java`
- Create: `backend/src/main/java/com/gymmind/shared/api/PageResponse.java`
- Create: `backend/src/main/java/com/gymmind/shared/api/TraceIdFilter.java`
- Create: `backend/src/main/java/com/gymmind/shared/error/ErrorCode.java`
- Create: `backend/src/main/java/com/gymmind/shared/error/BusinessException.java`
- Create: `backend/src/main/java/com/gymmind/shared/error/GlobalExceptionHandler.java`
- Test: `backend/src/test/java/com/gymmind/shared/api/ApiResponseTest.java`
- Test: `backend/src/test/java/com/gymmind/shared/api/TraceIdFilterTest.java`
- Test: `backend/src/test/java/com/gymmind/shared/error/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Produces: 所有 Controller 共用的 envelope；请求头 `X-Trace-Id` 仅接受 `[A-Za-z0-9_-]{8,64}`，否则生成 UUID。
- Consumes: Task 1 Web/Validation。

- [ ] **Step 1: 写失败测试**

```java
@Test
void validationErrorsUseStableEnvelope() throws Exception {
    mockMvc.perform(post("/test-validation").contentType(APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.traceId").isNotEmpty());
}
```

- [ ] **Step 2: 运行失败测试**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=ApiResponseTest,TraceIdFilterTest,GlobalExceptionHandlerTest test
```

- [ ] **Step 3: 实现固定错误码和映射**

`ErrorCode` 至少包含 `VALIDATION_FAILED(400)`、`UNAUTHENTICATED(401)`、`FORBIDDEN(403)`、`RESOURCE_NOT_FOUND(404)`、`CONFLICT(409)`、`DEPENDENCY_UNAVAILABLE(503)`、`INTERNAL_ERROR(500)`；异常响应不得回传堆栈、SQL 或秘密。

- [ ] **Step 4: 回归并提交**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=ApiResponseTest,TraceIdFilterTest,GlobalExceptionHandlerTest test
git add backend/src/main/java/com/gymmind/shared/api backend/src/main/java/com/gymmind/shared/error backend/src/test/java/com/gymmind/shared/api backend/src/test/java/com/gymmind/shared/error
git commit -m "feat: 统一API响应与异常处理"
```

### Task 4: 建立 JPA 基类与 MySQL 容器测试

**Files:**
- Create: `backend/src/main/java/com/gymmind/shared/persistence/BaseEntity.java`
- Create: `backend/src/main/java/com/gymmind/shared/persistence/AuditableEntity.java`
- Create: `backend/src/main/java/com/gymmind/shared/persistence/TenantScopedEntity.java`
- Create: `backend/src/test/java/com/gymmind/support/MySqlIntegrationTest.java`
- Test: `backend/src/test/java/com/gymmind/shared/persistence/TenantScopedEntityMappingIT.java`

**Interfaces:**
- Produces: Long 自增 ID、`@Version long version`、UTC `createdAt/updatedAt`、不可为空 tenantId。
- Consumes: Task 2 JPA 审计配置。

- [ ] **Step 1: 写真实 MySQL 映射测试**

```java
class TenantScopedEntityMappingIT extends MySqlIntegrationTest {
    @Test
    void tenantIdIsNotNullable() {
        assertThatThrownBy(() -> entityManager.persistAndFlush(new ProbeEntity(null)))
                .isInstanceOf(PersistenceException.class);
    }
}
```

- [ ] **Step 2: 运行并确认缺少基类或表映射而失败**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Pintegration -Dit.test=TenantScopedEntityMappingIT verify
```

- [ ] **Step 3: 实现映射和容器支持**

`MySqlIntegrationTest` 使用 `MySQLContainer<>("mysql:8.0.41")`、数据库名 `gymmind`、`@ServiceConnection`；不得读取本机数据库环境变量。

- [ ] **Step 4: 验证并提交**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Pintegration -Dit.test=TenantScopedEntityMappingIT verify
git add backend/src/main/java/com/gymmind/shared/persistence backend/src/test/java/com/gymmind/shared/persistence backend/src/test/java/com/gymmind/support backend/pom.xml
git commit -m "test: 建立MySQL容器化持久层基线"
```

### Task 5: 实现 Tenant 与 TenantSettings

**Files:**
- Create: `backend/src/main/java/com/gymmind/tenancy/domain/model/{Tenant,TenantStatus,TenantSettings}.java`
- Create: `backend/src/main/java/com/gymmind/tenancy/domain/repository/{TenantRepository,TenantSettingsRepository}.java`
- Create: `backend/src/main/java/com/gymmind/tenancy/infrastructure/persistence/{SpringDataTenantRepository,SpringDataTenantSettingsRepository,JpaTenantRepository,JpaTenantSettingsRepository}.java`
- Test: `backend/src/test/java/com/gymmind/tenancy/domain/TenantTest.java`
- Test: `backend/src/test/java/com/gymmind/tenancy/infrastructure/{TenantRepositoryIT,TenantSettingsRepositoryIT}.java`

**Interfaces:**
- Produces: `Tenant.create(code,name)`、`activate()`、`disable()`；每租户一份预约/签到/时区设置。
- Consumes: Task 4 JPA 基类。

- [ ] **Step 1: 写状态机和唯一约束测试**

```java
@Test
void disabledTenantIsNotActive() {
    Tenant tenant = Tenant.create("power-house", "Power House");
    tenant.disable();
    assertThat(tenant.isActive()).isFalse();
}
```

Repository IT 验证 code 全局唯一、`sys_tenant_settings.tenant_id` 唯一且非空。

- [ ] **Step 2: 运行红灯**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=TenantTest test
backend\mvnw.cmd -f backend/pom.xml -Pintegration -Dit.test=TenantRepositoryIT,TenantSettingsRepositoryIT verify
```

- [ ] **Step 3: 实现实体和显式仓储端口**

```java
public interface TenantRepository {
    Tenant save(Tenant tenant);
    Optional<Tenant> findById(Long id);
    Optional<Tenant> findByCode(String normalizedCode);
    boolean existsByCode(String normalizedCode);
    Page<Tenant> findAll(Pageable pageable);
}

public interface TenantSettingsRepository {
    TenantSettings save(TenantSettings settings);
    Optional<TenantSettings> findByTenantId(Long tenantId);
}
```

- [ ] **Step 4: 绿色验证与提交**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=TenantTest test
backend\mvnw.cmd -f backend/pom.xml -Pintegration -Dit.test=TenantRepositoryIT,TenantSettingsRepositoryIT verify
git add backend/src/main/java/com/gymmind/tenancy backend/src/test/java/com/gymmind/tenancy
git commit -m "feat: 建立租户与租户设置模型"
```

### Task 6: 实现 IAM 用户、角色与权限模型

**Files:**
- Create: `backend/src/main/java/com/gymmind/iam/domain/model/{UserAccount,UserStatus,Role,RoleCode,Permission,UserRole,RolePermission}.java`
- Create: `backend/src/main/java/com/gymmind/iam/domain/repository/{UserAccountRepository,RoleRepository,PermissionRepository}.java`
- Create: `backend/src/main/java/com/gymmind/iam/infrastructure/persistence/Jpa*.java`
- Test: `backend/src/test/java/com/gymmind/iam/domain/UserAccountTest.java`
- Test: `backend/src/test/java/com/gymmind/iam/infrastructure/{UserAccountRepositoryIT,RolePermissionRepositoryIT}.java`

**Interfaces:**
- Produces: 固定角色 `PLATFORM_ADMIN/GYM_ADMIN/COACH/MEMBER` 和动态权限集合。
- Consumes: Tenant ID 与 JPA 基类。

- [ ] **Step 1: 写用户边界测试**

```java
@Test
void tenantUserRequiresTenantButPlatformAdminDoesNot() {
    assertThatThrownBy(() -> UserAccount.tenantUser(null, "admin@example.com", HASH, "Admin"))
            .isInstanceOf(IllegalArgumentException.class);
    assertThat(UserAccount.platformAdmin("root@example.com", HASH, "Root").getTenantId()).isNull();
}
```

IT 验证规范化邮箱全局唯一、租户查询使用 `findByTenantIdAndId`、另一 tenantId 返回空。

- [ ] **Step 2: 运行红灯**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=UserAccountTest test
backend\mvnw.cmd -f backend/pom.xml -Pintegration -Dit.test=UserAccountRepositoryIT,RolePermissionRepositoryIT verify
```

- [ ] **Step 3: 实现模型和端口**

```java
public interface UserAccountRepository {
    UserAccount save(UserAccount user);
    Optional<UserAccount> findByNormalizedEmail(String normalizedEmail);
    Optional<UserAccount> findByTenantIdAndId(Long tenantId, Long id);
    Page<UserAccount> findAllByTenantId(Long tenantId, Pageable pageable);
}
```

禁用账户、修改关键角色和密码重置必须调用 `incrementTokenVersion()`。

- [ ] **Step 4: 验证并提交**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=UserAccountTest test
backend\mvnw.cmd -f backend/pom.xml -Pintegration -Dit.test=UserAccountRepositoryIT,RolePermissionRepositoryIT verify
git add backend/src/main/java/com/gymmind/iam/domain backend/src/main/java/com/gymmind/iam/infrastructure backend/src/test/java/com/gymmind/iam
git commit -m "feat: 建立IAM用户角色权限模型"
```

### Task 7: 幂等初始化系统目录与平台管理员

**Files:**
- Create: `backend/src/main/java/com/gymmind/platform/bootstrap/{PermissionCatalog,SystemCatalogInitializer,PlatformAdminProperties,PlatformAdminInitializer}.java`
- Create: `backend/src/main/java/com/gymmind/tenancy/application/{TenantProvisioningService,DefaultTenantProvisioningService,TenantSettingsProvisioningContributor}.java`
- Create: `backend/src/main/java/com/gymmind/tenancy/application/port/TenantProvisioningContributor.java`
- Test: `backend/src/test/java/com/gymmind/platform/bootstrap/{SystemCatalogInitializerIT,PlatformAdminInitializerIT}.java`

**Interfaces:**
- Produces: 幂等角色/权限目录、可选平台管理员、可扩展租户初始化贡献者。
- Consumes: Tasks 5-6 repositories。

- [ ] **Step 1: 写两次执行不重复的测试**

```java
@Test
void catalogInitializationIsIdempotent() {
    initializer.run();
    initializer.run();
    assertThat(roleRepository.count()).isEqualTo(4);
    assertThat(permissionRepository.count()).isEqualTo(PermissionCatalog.entries().size());
}
```

- [ ] **Step 2: 运行并看到目录未初始化的失败**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Pintegration -Dit.test=SystemCatalogInitializerIT,PlatformAdminInitializerIT verify
```

- [ ] **Step 3: 实现固定权限目录**

初始权限必须包括 `platform:tenant:read/write`、`tenant:settings:read/write`、`user:read/write`、`role:read/assign`。平台管理员仅在 `PLATFORM_ADMIN_EMAIL` 与 `PLATFORM_ADMIN_PASSWORD` 同时存在时创建，密码用 BCrypt 且日志不打印值。

- [ ] **Step 4: 验证并提交**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Pintegration -Dit.test=SystemCatalogInitializerIT,PlatformAdminInitializerIT verify
git add backend/src/main/java/com/gymmind/platform/bootstrap backend/src/main/java/com/gymmind/tenancy/application backend/src/test/java/com/gymmind/platform/bootstrap
git commit -m "feat: 实现系统目录与平台管理员幂等初始化"
```

### Task 8: 建立 CurrentActor 与租户访问守卫

**Files:**
- Create: `backend/src/main/java/com/gymmind/shared/security/{CurrentActor,CurrentActorProvider,SecurityContextCurrentActorProvider,GymMindPrincipal,TenantAccessGuard}.java`
- Create: `backend/src/test/java/com/gymmind/support/SecurityTestActors.java`
- Test: `backend/src/test/java/com/gymmind/shared/security/{CurrentActorTest,SecurityContextCurrentActorProviderTest,TenantAccessGuardTest}.java`

**Interfaces:**
- Produces: 所有应用服务的可信 actor 和 fail-closed tenant guard。
- Consumes: Task 6 角色/权限语义。

- [ ] **Step 1: 写匿名、平台、错租户和缺权限失败测试**

```java
@Test
void platformActorCannotEnterTenantBusinessScope() {
    CurrentActor platform = SecurityTestActors.platformAdmin();
    assertThatThrownBy(() -> guard.requireTenant(platform))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
}
```

- [ ] **Step 2: 运行红灯**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=CurrentActorTest,SecurityContextCurrentActorProviderTest,TenantAccessGuardTest test
```

- [ ] **Step 3: 实现记录类型和 Provider**

`SecurityContextCurrentActorProvider.requireCurrent()` 对空认证、匿名主体或非 `GymMindPrincipal` 一律抛 `UNAUTHENTICATED`；`requireSameTenant` 不匹配时对资源操作抛 `RESOURCE_NOT_FOUND`，避免枚举 ID。

- [ ] **Step 4: 绿色验证与提交**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=CurrentActorTest,SecurityContextCurrentActorProviderTest,TenantAccessGuardTest test
git add backend/src/main/java/com/gymmind/shared/security backend/src/test/java/com/gymmind/shared/security backend/src/test/java/com/gymmind/support
git commit -m "feat: 建立认证主体与租户访问守卫"
```

### Task 9: 实现 JWT 编解码

**Files:**
- Create: `backend/src/main/java/com/gymmind/shared/security/jwt/{JwtService,JjwtJwtService,JwtClaims,TokenPair,TokenType,TokenIdGenerator}.java`
- Test: `backend/src/test/java/com/gymmind/shared/security/jwt/JwtServiceTest.java`

**Interfaces:**
- Produces: 可测试的访问/刷新 Token pair 和严格校验的 claims。
- Consumes: SecurityProperties、CurrentActor。

- [ ] **Step 1: 写确定性时钟测试**

```java
@Test
void rejectsRefreshTokenWhenAccessTokenExpected() {
    TokenPair pair = jwtService.issue(ACTOR);
    assertThatThrownBy(() -> jwtService.verify(pair.refreshToken(), TokenType.ACCESS))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHENTICATED);
}
```

同一测试类覆盖 issuer、userId、tenantId、roles、permissions、tokenVersion、jti、类型、签名篡改和过期。

- [ ] **Step 2: 运行红灯**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=JwtServiceTest test
```

- [ ] **Step 3: 实现接口**

```java
public interface JwtService {
    TokenPair issue(CurrentActor actor);
    JwtClaims verify(String rawToken, TokenType expectedType);
}

public record TokenPair(
        String accessToken, Instant accessExpiresAt,
        String refreshToken, Instant refreshExpiresAt) {}
```

注入 `Clock` 和 `TokenIdGenerator`；解析异常统一映射为 `UNAUTHENTICATED`，不得把 token 放入异常消息。

- [ ] **Step 4: 验证与提交**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=JwtServiceTest test
git add backend/src/main/java/com/gymmind/shared/security/jwt backend/src/test/java/com/gymmind/shared/security/jwt backend/pom.xml
git commit -m "feat: 实现访问令牌与刷新令牌"
```

### Task 10: 实现 Redis 会话轮换与撤销

**Files:**
- Create: `backend/src/main/java/com/gymmind/iam/application/model/RefreshSession.java`
- Create: `backend/src/main/java/com/gymmind/iam/application/port/SessionStore.java`
- Create: `backend/src/main/java/com/gymmind/iam/application/SessionService.java`
- Create: `backend/src/main/java/com/gymmind/iam/infrastructure/redis/RedisSessionStore.java`
- Test: `backend/src/test/java/com/gymmind/iam/application/SessionServiceTest.java`
- Test: `backend/src/test/java/com/gymmind/iam/infrastructure/RedisSessionStoreIT.java`

**Interfaces:**
- Produces: refresh 单次消费、访问令牌撤销、TTL 和带租户命名空间 Redis key。
- Consumes: JwtService tokenId/expiry。

- [ ] **Step 1: 写轮换和 fail-closed 测试**

```java
@Test
void refreshTokenCanBeConsumedOnlyOnce() {
    service.store(SESSION, RAW_REFRESH);
    assertThat(service.consume(SESSION.tokenId(), RAW_REFRESH)).isTrue();
    assertThat(service.consume(SESSION.tokenId(), RAW_REFRESH)).isFalse();
}
```

- [ ] **Step 2: 运行 unit 与 Redis 容器红灯**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=SessionServiceTest test
backend\mvnw.cmd -f backend/pom.xml -Pintegration -Dit.test=RedisSessionStoreIT verify
```

- [ ] **Step 3: 实现会话端口**

```java
public interface SessionStore {
    void storeRefresh(RefreshSession session, Duration ttl);
    boolean consumeRefresh(String namespacedTokenId, String tokenHash);
    void revokeAccess(String namespacedTokenId, Duration ttl);
    boolean isAccessRevoked(String namespacedTokenId);
    void deleteRefresh(String namespacedTokenId);
}
```

键格式固定 `gymmind:{env}:v1:auth:{tenant-or-platform}:{userId}:{kind}:{tokenId}`；refresh 只保存 SHA-256 摘要，不存明文。

- [ ] **Step 4: 验证并提交**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=SessionServiceTest test
backend\mvnw.cmd -f backend/pom.xml -Pintegration -Dit.test=RedisSessionStoreIT verify
git add backend/src/main/java/com/gymmind/iam backend/src/test/java/com/gymmind/iam
git commit -m "feat: 实现令牌会话轮换与撤销"
```

### Task 11: 实现租户注册、登录、刷新和退出

**Files:**
- Create: `backend/src/main/java/com/gymmind/iam/application/AuthApplicationService.java`
- Create: `backend/src/main/java/com/gymmind/iam/application/DefaultAuthApplicationService.java`
- Create: `backend/src/main/java/com/gymmind/iam/application/command/{RegisterTenantCommand,LoginCommand,RefreshCommand,LogoutCommand}.java`
- Create: `backend/src/main/java/com/gymmind/iam/application/result/AuthResult.java`
- Create: `backend/src/main/java/com/gymmind/tenancy/application/TenantRegistrationService.java`
- Test: `backend/src/test/java/com/gymmind/iam/application/AuthApplicationServiceTest.java`
- Test: `backend/src/test/java/com/gymmind/tenancy/application/TenantRegistrationServiceIT.java`

**Interfaces:**
- Produces: 单事务公开注册和认证用例；公开注册无法选择角色。
- Consumes: Tenant、IAM、provisioning、BCrypt、JWT、SessionStore。

- [ ] **Step 1: 写事务和禁用状态测试**

```java
@Test
void publicRegistrationAlwaysCreatesGymAdmin() {
    AuthResult result = service.registerTenant(REGISTER_COMMAND);
    assertThat(result.user().roles()).containsExactly(RoleCode.GYM_ADMIN);
}
```

IT 人为让 provisioning contributor 失败，断言 Tenant、User、UserRole、Settings 全部回滚；禁用用户或租户不能登录/刷新。

- [ ] **Step 2: 运行红灯**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=AuthApplicationServiceTest test
backend\mvnw.cmd -f backend/pom.xml -Pintegration -Dit.test=TenantRegistrationServiceIT verify
```

- [ ] **Step 3: 实现接口**

```java
public interface AuthApplicationService {
    AuthResult registerTenant(RegisterTenantCommand command);
    AuthResult login(LoginCommand command);
    AuthResult refresh(RefreshCommand command);
    void logout(CurrentActor actor, LogoutCommand command);
}
```

邮箱按 trim/lowercase 规范化；登录错误统一为相同 401 信息；刷新同时复核 tokenVersion、用户 ACTIVE、Tenant ACTIVE 并轮换 token。

- [ ] **Step 4: 验证并提交**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=AuthApplicationServiceTest test
backend\mvnw.cmd -f backend/pom.xml -Pintegration -Dit.test=TenantRegistrationServiceIT verify
git add backend/src/main/java/com/gymmind/iam/application backend/src/main/java/com/gymmind/tenancy/application backend/src/test/java/com/gymmind/iam backend/src/test/java/com/gymmind/tenancy/application
git commit -m "feat: 实现租户注册与认证用例"
```

### Task 12: 接入 JWT Filter、RBAC 与认证 API

**Files:**
- Create: `backend/src/main/java/com/gymmind/shared/security/{SecurityConfig,JwtAuthenticationFilter,PrincipalLoader,RestAuthenticationEntryPoint,RestAccessDeniedHandler}.java`
- Create: `backend/src/main/java/com/gymmind/iam/api/AuthController.java`
- Create: `backend/src/main/java/com/gymmind/iam/api/request/{RegisterRequest,LoginRequest,RefreshRequest,LogoutRequest}.java`
- Create: `backend/src/main/java/com/gymmind/iam/api/response/AuthResponse.java`
- Test: `backend/src/test/java/com/gymmind/shared/security/{JwtAuthenticationFilterTest,SecurityConfigurationTest}.java`
- Test: `backend/src/test/java/com/gymmind/iam/api/AuthControllerTest.java`

**Interfaces:**
- Produces: `/api/v1/auth/register|login|refresh|logout` 与 bearer JWT 安全链。
- Consumes: AuthApplicationService、JwtService、SessionStore、User/Tenant repositories。

- [ ] **Step 1: 写 public/secured 矩阵测试**

```java
@Test
void invalidBearerTokenReturnsUnified401() throws Exception {
    mockMvc.perform(get("/api/v1/test-secured").header(AUTHORIZATION, "Bearer invalid"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
}
```

只放行 register/login/refresh、`/actuator/health` 和 OpenAPI；logout 必须认证。

- [ ] **Step 2: 运行红灯**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=JwtAuthenticationFilterTest,SecurityConfigurationTest,AuthControllerTest test
```

- [ ] **Step 3: 实现 Filter 与 Controller**

`PrincipalLoader.load(JwtClaims)` 每个请求复核 access 类型、Redis jti 未撤销、用户 tokenVersion、用户状态、租户状态；任一依赖异常或状态不明 fail closed。

- [ ] **Step 4: 验证并提交**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=JwtAuthenticationFilterTest,SecurityConfigurationTest,AuthControllerTest test
git add backend/src/main/java/com/gymmind/shared/security backend/src/main/java/com/gymmind/iam/api backend/src/test/java/com/gymmind/shared/security backend/src/test/java/com/gymmind/iam/api
git commit -m "feat: 接入JWT认证过滤器与RBAC"
```

### Task 13: 实现平台租户管理和当前租户设置

**Files:**
- Create: `backend/src/main/java/com/gymmind/tenancy/application/{PlatformTenantService,TenantSettingsService}.java`
- Create: `backend/src/main/java/com/gymmind/tenancy/api/{PlatformTenantController,TenantSettingsController}.java`
- Create: `backend/src/main/java/com/gymmind/tenancy/api/{request,response}/**`
- Test: `backend/src/test/java/com/gymmind/tenancy/api/{PlatformTenantControllerTest,TenantSettingsControllerTest,TenantBoundaryIT}.java`

**Interfaces:**
- Produces: `/api/v1/platform/tenants` 和 `/api/v1/tenant/settings`。
- Consumes: CurrentActorProvider、Tenant repositories、ProvisioningService。

- [ ] **Step 1: 写四角色边界测试**

```java
@Test
void platformAdminCannotReadTenantSettings() throws Exception {
    mockMvc.perform(get("/api/v1/tenant/settings").with(platformAdmin()))
            .andExpect(status().isForbidden());
}
```

仅平台管理员可创建、分页和启停 Tenant；仅当前 `GYM_ADMIN` 可更新自己的 settings；tenantId 不从请求读取。

- [ ] **Step 2: 运行红灯**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=PlatformTenantControllerTest,TenantSettingsControllerTest test
backend\mvnw.cmd -f backend/pom.xml -Pintegration -Dit.test=TenantBoundaryIT verify
```

- [ ] **Step 3: 实现服务/API 并在禁用租户时使旧令牌立即失效**

```java
public interface TenantSettingsService {
    TenantSettingsView getCurrent();
    TenantSettingsView updateCurrent(UpdateTenantSettingsCommand command);
}
```

- [ ] **Step 4: 验证并提交**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=PlatformTenantControllerTest,TenantSettingsControllerTest test
backend\mvnw.cmd -f backend/pom.xml -Pintegration -Dit.test=TenantBoundaryIT verify
git add backend/src/main/java/com/gymmind/tenancy backend/src/test/java/com/gymmind/tenancy/api
git commit -m "feat: 实现平台租户管理与租户设置"
```

### Task 14: 实现租户用户管理与角色分配

**Files:**
- Create: `backend/src/main/java/com/gymmind/iam/application/UserAdministrationService.java`
- Create: `backend/src/main/java/com/gymmind/iam/api/{UserController,RoleController}.java`
- Create: `backend/src/main/java/com/gymmind/iam/api/{request,response}/**`
- Test: `backend/src/test/java/com/gymmind/iam/application/UserAdministrationServiceTest.java`
- Test: `backend/src/test/java/com/gymmind/iam/api/{UserControllerTest,UserTenantIsolationIT}.java`

**Interfaces:**
- Produces: `/api/v1/users`、`/api/v1/roles`。
- Consumes: CurrentActor、User/Role repository、tokenVersion。

- [ ] **Step 1: 写权限提升和跨租户测试**

```java
@Test
void gymAdminCannotGrantPlatformOrGymAdminRole() {
    assertThatThrownBy(() -> service.replaceRoles(GYM_ADMIN, USER_ID, Set.of(RoleCode.PLATFORM_ADMIN)))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
}
```

GYM_ADMIN 只能管理本租户 COACH/MEMBER；跨租户 ID 返回 404；状态或角色变更递增 tokenVersion。

- [ ] **Step 2: 运行红灯**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=UserAdministrationServiceTest,UserControllerTest test
backend\mvnw.cmd -f backend/pom.xml -Pintegration -Dit.test=UserTenantIsolationIT verify
```

- [ ] **Step 3: 实现服务契约**

```java
public interface UserAdministrationService {
    UserView create(CurrentActor actor, CreateUserCommand command);
    PageResponse<UserSummary> list(CurrentActor actor, Pageable pageable);
    void changeStatus(CurrentActor actor, Long userId, UserStatus status);
    void replaceRoles(CurrentActor actor, Long userId, Set<RoleCode> roles);
}
```

- [ ] **Step 4: 验证并提交**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=UserAdministrationServiceTest,UserControllerTest test
backend\mvnw.cmd -f backend/pom.xml -Pintegration -Dit.test=UserTenantIsolationIT verify
git add backend/src/main/java/com/gymmind/iam backend/src/test/java/com/gymmind/iam
git commit -m "feat: 实现租户用户管理与角色分配"
```

### Task 15: 实现教练和会员邀请

**Files:**
- Create: `backend/src/main/java/com/gymmind/iam/domain/model/{UserInvitation,InvitationStatus}.java`
- Create: `backend/src/main/java/com/gymmind/iam/domain/repository/UserInvitationRepository.java`
- Create: `backend/src/main/java/com/gymmind/iam/application/UserInvitationService.java`
- Create: `backend/src/main/java/com/gymmind/iam/api/{UserInvitationController,AuthInvitationController}.java`
- Test: `backend/src/test/java/com/gymmind/iam/application/UserInvitationServiceTest.java`
- Test: `backend/src/test/java/com/gymmind/iam/infrastructure/UserInvitationRepositoryIT.java`
- Test: `backend/src/test/java/com/gymmind/iam/api/UserInvitationControllerTest.java`

**Interfaces:**
- Produces: `POST /api/v1/users/invitations` 和 `POST /api/v1/auth/invitations/accept`。
- Consumes: GYM_ADMIN actor、Tenant/User/Role repositories、BCrypt、JwtService。

- [ ] **Step 1: 写邀请生命周期测试**

```java
@Test
void invitationCanBeAcceptedOnlyOnce() {
    InvitationIssued issued = service.invite(GYM_ADMIN, INVITE_MEMBER);
    service.accept(new AcceptInvitationCommand(issued.rawToken(), "ValidPassword123!", "Member"));
    assertThatThrownBy(() -> service.accept(new AcceptInvitationCommand(issued.rawToken(), "AnotherPassword123!", "Member")))
            .isInstanceOf(BusinessException.class);
}
```

- [ ] **Step 2: 运行红灯**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=UserInvitationServiceTest,UserInvitationControllerTest test
backend\mvnw.cmd -f backend/pom.xml -Pintegration -Dit.test=UserInvitationRepositoryIT verify
```

- [ ] **Step 3: 实现只存摘要的邀请**

```java
public interface UserInvitationService {
    InvitationIssued invite(CurrentActor actor, InviteUserCommand command);
    AuthResult accept(AcceptInvitationCommand command);
}
```

只允许目标角色 COACH/MEMBER；过期、已使用、Tenant 禁用或邮箱已注册均拒绝；原始 token 只在创建响应中返回一次。

- [ ] **Step 4: 验证并提交**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=UserInvitationServiceTest,UserInvitationControllerTest test
backend\mvnw.cmd -f backend/pom.xml -Pintegration -Dit.test=UserInvitationRepositoryIT verify
git add backend/src/main/java/com/gymmind/iam backend/src/test/java/com/gymmind/iam
git commit -m "feat: 实现租户用户邀请流程"
```

### Task 16: 固化租户隔离架构和 IDOR 边界

**Files:**
- Create: `backend/src/test/java/com/gymmind/architecture/TenantIsolationArchitectureTest.java`
- Create: `backend/src/test/java/com/gymmind/security/TenantIdorSecurityIT.java`
- Modify: Tasks 5-15 中不满足规则的 repository port/adapter 可见性。

**Interfaces:**
- Produces: 自动阻止后续模块引入无 tenantId 查询或客户端 tenantId。
- Consumes: 完整 tenancy/IAM 第一阶段实现。

- [ ] **Step 1: 写 ArchUnit 失败规则**

```java
@ArchTest
static final ArchRule apiMustNotDependOnRepositories = noClasses()
        .that().resideInAPackage("..api..")
        .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository");
```

另用反射扫描租户请求 DTO 禁止 `tenantId` 字段，并扫描租户 repository port 禁止暴露 `findById/deleteById/existsById`。

- [ ] **Step 2: 运行并记录实际违规项**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=TenantIsolationArchitectureTest test
backend\mvnw.cmd -f backend/pom.xml -Pintegration -Dit.test=TenantIdorSecurityIT verify
```

- [ ] **Step 3: 收紧 adapter 为 package-private 并补齐显式 tenant 查询**

两租户使用相同局部键创建数据；列表、读取、修改、删除、禁用和角色变更另一租户 ID 全部返回 404，平台管理员返回 403。

- [ ] **Step 4: 验证并提交**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=TenantIsolationArchitectureTest test
backend\mvnw.cmd -f backend/pom.xml -Pintegration -Dit.test=TenantIdorSecurityIT verify
git add backend/src/main/java/com/gymmind backend/src/test/java/com/gymmind/architecture backend/src/test/java/com/gymmind/security
git commit -m "test: 固化租户隔离架构与IDOR边界"
```

### Task 17: 添加并执行 gymmind 数据库安全重置

**Files:**
- Create: `backend/src/main/java/com/gymmind/platform/database/DatabaseNameGuard.java`
- Create: `backend/scripts/reset-gymmind-db.ps1`
- Create: `backend/scripts/reset-gymmind-db.sh`
- Create: `backend/docs/database-reset.md`
- Test: `backend/src/test/java/com/gymmind/platform/database/{DatabaseNameGuardTest,DatabaseResetScriptContractTest,DatabaseResetIT}.java`

**Interfaces:**
- Produces: 默认 dry-run、必须输入 `DROP gymmind` 的固定目标重置工具。
- Consumes: MySQL 8 CLI 与通过环境/交互提供的凭据。

- [ ] **Step 1: 写名称和脚本契约测试**

```java
@ParameterizedTest
@NullAndEmptySource
@ValueSource(strings = {"GymMind", "mysql", "information_schema", "gymmind ", "gymmind;DROP DATABASE mysql"})
void rejectsEveryDatabaseExceptExactGymmind(String name) {
    assertThatThrownBy(() -> DatabaseNameGuard.requireAllowed(name))
            .isInstanceOf(IllegalArgumentException.class);
}
```

脚本契约测试确认仅出现固定 SQL `DROP DATABASE IF EXISTS gymmind` 与 `CREATE DATABASE gymmind CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci`，没有动态 SQL 标识符拼接。

- [ ] **Step 2: 运行红灯**

```powershell
backend\mvnw.cmd -f backend/pom.xml -Dtest=DatabaseNameGuardTest,DatabaseResetScriptContractTest test
backend\mvnw.cmd -f backend/pom.xml -Pintegration -Dit.test=DatabaseResetIT verify
```

- [ ] **Step 3: 实现护栏并先做本机只读核验**

```powershell
mysql --host=localhost --port=3306 --user=$env:MYSQL_USER --password -N -e "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME='gymmind';"
```

不得在命令行参数、日志或输出中展开密码。只有返回精确 `gymmind` 后才运行脚本，并交互输入完整确认短语。

- [ ] **Step 4: 执行重置并核验其他库未变化**

```powershell
backend\scripts\reset-gymmind-db.ps1 -HostName localhost -Port 3306 -UserName $env:MYSQL_USER
mysql --host=localhost --port=3306 --user=$env:MYSQL_USER --password -N -e "SHOW CREATE DATABASE gymmind;"
```

- [ ] **Step 5: 提交工具，不提交凭据或执行输出**

```powershell
git add backend/src/main/java/com/gymmind/platform/database backend/src/test/java/com/gymmind/platform/database backend/scripts/reset-gymmind-db.ps1 backend/scripts/reset-gymmind-db.sh backend/docs/database-reset.md
git commit -m "feat: 添加gymmind数据库安全重置工具"
```

### Task 18: 结构契约、OpenAPI 与第一阶段启动验收

**Files:**
- Create: `backend/src/main/java/com/gymmind/shared/config/OpenApiConfig.java`
- Create: `backend/src/test/resources/schema/phase-one-schema.json`
- Create: `backend/src/test/java/com/gymmind/platform/database/PhaseOneSchemaContractIT.java`
- Create: `backend/src/test/java/com/gymmind/shared/api/OpenApiContractIT.java`
- Create: `backend/src/test/java/com/gymmind/PhaseOneSmokeIT.java`
- Create: `backend/README.md`

**Interfaces:**
- Produces: 结构快照、bearer OpenAPI、从空库注册到退出的启动证据。
- Consumes: Tasks 1-17 全部能力。

- [ ] **Step 1: 写完整冒烟失败测试**

```java
@Test
void twoTenantsRemainIsolatedAcrossFullAuthLifecycle() {
    AuthTokens tenantA = register("tenant-a", "a@example.com");
    AuthTokens tenantB = register("tenant-b", "b@example.com");
    updateSettings(tenantA.accessToken(), "Asia/Shanghai");
    assertForbiddenOrNotFound(readTenantAUserWith(tenantB.accessToken()));
    AuthTokens rotated = refresh(tenantA.refreshToken());
    logout(rotated.accessToken(), rotated.refreshToken());
    assertUnauthorized(use(rotated.accessToken()));
}
```

结构快照验证 `sys_tenant`、`sys_tenant_settings`、`sys_user`、`sys_role`、`sys_permission`、`sys_user_role`、`sys_role_permission`、`sys_user_invitation` 的列、非空、组合索引和外键。OpenAPI 所有业务 path 必须以 `/api/v1` 开头并声明 bearerAuth。

- [ ] **Step 2: 运行全套测试并确认缺失契约**

```powershell
backend\mvnw.cmd -f backend/pom.xml clean test
backend\mvnw.cmd -f backend/pom.xml -Pintegration verify
```

- [ ] **Step 3: 补齐 OpenAPI、结构快照和运行文档**

```java
@Bean
OpenAPI gymMindOpenApi() {
    return new OpenAPI()
            .info(new Info().title("GymMind API").version("v1"))
            .components(new Components().addSecuritySchemes("bearerAuth",
                    new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
}
```

- [ ] **Step 4: 最终验证、打包和真实 dev 启动**

```powershell
backend\mvnw.cmd -f backend/pom.xml clean test
backend\mvnw.cmd -f backend/pom.xml -Pintegration verify
backend\mvnw.cmd -f backend/pom.xml package
backend\mvnw.cmd -f backend/pom.xml spring-boot:run
```

启动后验证 `/actuator/health` 为 `UP`，注册两个租户、登录、刷新、退出、禁用租户和跨租户拒绝；启动进程保持运行并向用户提供 URL。

- [ ] **Step 5: 提交**

```powershell
git add backend/src/main/java/com/gymmind/shared/config/OpenApiConfig.java backend/src/test/resources/schema/phase-one-schema.json backend/src/test/java/com/gymmind/platform/database/PhaseOneSchemaContractIT.java backend/src/test/java/com/gymmind/shared/api/OpenApiContractIT.java backend/src/test/java/com/gymmind/PhaseOneSmokeIT.java backend/README.md
git commit -m "test: 完成第一阶段结构契约与启动验收"
```

## Stage Verification

- [ ] `backend\mvnw.cmd -f backend/pom.xml test` 无 Docker 时通过。
- [ ] `backend\mvnw.cmd -f backend/pom.xml -Pintegration verify` 只连接 Testcontainers，不连接本机开发库。
- [ ] 本机准确名称为 `gymmind` 的数据库已重建，其他 schema 清单前后相同。
- [ ] Hibernate 从空库建立结构且没有 Flyway 表；初始化连续执行两次不重复。
- [ ] 平台管理员 tenantId 为 null 且不能进入租户明细 API。
- [ ] 所有租户实体查询显式携带 tenantId，跨租户 IDOR 测试通过。
- [ ] Git 暂存和历史中不存在 JWT、MySQL、平台管理员或百炼真实密钥。
- [ ] 后端在 Java 17 下保持运行，`http://localhost:8080/actuator/health` 返回 `UP`。
