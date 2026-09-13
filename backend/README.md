# GymMind Backend

GymMind 后端是 Java 17 + Spring Boot 3.4.12 应用，使用 MySQL 8 保存租户和 IAM 记录，Redis 保存刷新会话与访问令牌撤销状态。每个租户代表独立健身房，业务范围始终来自 JWT，不接受客户端传入 `tenantId`。

## 启动

开发环境默认使用 `dev` profile、端口 `8080`、MySQL 数据库 `gymmind` 和 Hibernate `ddl-auto=update`。启动前设置以下环境变量：

```powershell
$env:MYSQL_HOST = 'localhost'
$env:MYSQL_PORT = '3306'
$env:MYSQL_USER = 'gymmind_app'
$env:MYSQL_PASSWORD = '<local-secret>'
$env:JWT_SECRET = '<at-least-32-byte-secret>'
$env:REDIS_HOST = 'localhost'
$env:REDIS_PORT = '6379'
```

首次部署或需要平台管理员时，再提供一次性初始化值：

```powershell
$env:PLATFORM_ADMIN_EMAIL = 'platform-admin@example.com'
$env:PLATFORM_ADMIN_PASSWORD = '<one-time-password>'
```

不要把实际凭据写入仓库、配置文件、测试、日志或命令历史。应用启动会校验 JWT secret 长度；平台管理员配置为空时不会自动创建平台账号。

```powershell
$env:JAVA_HOME = 'D:\java\jdk-17.0.14'
./mvnw.cmd spring-boot:run
```

启动后检查：

```text
GET http://localhost:8080/actuator/health
```

健康接口应返回 `{"status":"UP"}`。OpenAPI 文档位于 `/swagger-ui.html`，JSON 位于 `/v3/api-docs`；业务接口统一使用 `/api/v1`。

## 测试

无 Docker 时运行普通测试：

```powershell
./mvnw.cmd test
```

MySQL/Redis 集成测试使用 Testcontainers，不连接本机开发库：

```powershell
./mvnw.cmd -Pintegration verify
```

## 数据库重置

数据库重置脚本默认 dry-run，只有显式执行开关和确认短语 `DROP gymmind` 才会删除并重建精确小写的 `gymmind` 数据库。详见 [database-reset.md](docs/database-reset.md)。
