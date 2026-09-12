# Task 3 报告：统一 API 响应、异常与 traceId

## 实现内容

- 新增 `ApiResponse<T>` 固定 envelope 和 `PageResponse<T>` 分页结构；成功与错误响应均读取当前请求 MDC 中的 traceId。
- 新增七个固定 `ErrorCode` 及 `BusinessException`；业务异常响应只使用错误码定义的公开消息，不回显异常内部消息。
- 新增 `GlobalExceptionHandler`，统一映射 Bean Validation、请求体解析、参数绑定/类型转换、业务异常和未知异常。
- 未知异常响应固定为 `INTERNAL_ERROR`；服务端日志只记录异常类型，不记录异常原文、堆栈、SQL、token、密码或秘密。
- 新增 `TraceIdFilter`：仅复用完整匹配 `[A-Za-z0-9_-]{8,64}` 的 `X-Trace-Id`，其他值生成 UUID；同一值写入 request attribute、MDC 和响应头，并在 `finally` 清理。
- 对 Spring MVC `Callable` 注册上下文拦截器，异步工作线程与 async dispatch 复用同一 traceId，并分别清理线程上下文。
- 测试 Controller 路径全部位于 `/api/v1`，未实现后续认证链。

## 文件

- 生产：`ApiResponse.java`、`PageResponse.java`、`TraceIdFilter.java`、`ErrorCode.java`、`BusinessException.java`、`GlobalExceptionHandler.java`。
- 测试：`ApiResponseTest.java`、`TraceIdFilterTest.java`、`GlobalExceptionHandlerTest.java`。
- 报告：本文件。

## RED / GREEN

- 环境校正：直接执行 brief 命令时 PowerShell 需引用含逗号的 `-Dtest` 参数；系统默认 Java 8 不能读取 Spring Boot 3 类，因此测试统一临时设置 `JAVA_HOME=D:\java\jdk-17.0.14`。
- 初始 RED：`$env:JAVA_HOME='D:\java\jdk-17.0.14'; backend\mvnw.cmd -f backend/pom.xml '-Dtest=ApiResponseTest,TraceIdFilterTest,GlobalExceptionHandlerTest' test`；testCompile 报 27 个“找不到符号”，对应六个尚未实现的生产类型，符合预期。
- 初始 GREEN：同命令实现后通过，`Tests run: 8, Failures: 0, Errors: 0`。
- 审查补强 RED：`$env:JAVA_HOME='D:\java\jdk-17.0.14'; backend\mvnw.cmd -f backend/pom.xml '-Dtest=TraceIdFilterTest,GlobalExceptionHandlerTest' test`；`Tests run: 9, Failures: 2`，畸形 JSON 实际 500、异步 envelope traceId 实际为 null，符合新增测试预期。
- 最终聚焦 GREEN：brief 指定三类测试通过，`Tests run: 11, Failures: 0, Errors: 0`。
- 完整 unit suite：`$env:JAVA_HOME='D:\java\jdk-17.0.14'; backend\mvnw.cmd -f backend/pom.xml test`；`Tests run: 19, Failures: 0, Errors: 0`，未启用 integration profile，不依赖 Docker。

## 检查与自审

- 独立只读审查未发现 Critical；按 Important 修复异步 Callable trace 传播及畸形 JSON 400 映射，按 Minor 补齐完整响应脱敏和 traceId 长度/字符边界测试。
- 变异检查覆盖：合法值复用、非法字符及 7/8/64/65 边界、UUID 替换、同步正常/异常清理、Callable 传播、错误状态码、业务与未知异常原文脱敏。
- `git diff --check`、暂存范围检查和 staged secret scan 的结果在提交前执行并确认。

## 疑虑

- 当前显式异步传播覆盖 Spring MVC `Callable`；未来若引入由业务线程主动完成的 `DeferredResult`，需要在该业务执行器增加 MDC task decoration，不能依赖 Servlet filter 自动传播。
- Spring Security filter chain 中产生的认证/授权异常发生在 Controller advice 之外，应由后续认证任务使用相同 envelope 与 traceId 约定处理；本任务按约束未实现该链。
