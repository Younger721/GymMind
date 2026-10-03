# 角色权限按管理员作用域配置 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为平台管理员和健身房管理员提供隔离的角色菜单权限配置，并让门店覆盖在登录权限中生效。

**Architecture:** 保留全局 `sys_role_permission` 作为平台模板，新增租户角色权限关联表作为门店覆盖。权限加载按租户覆盖优先、平台模板回退解析；角色接口和前端页面按操作者作用域限制可编辑角色。

**Tech Stack:** Spring Boot 3.4、Spring Data JPA、MySQL、Vue 3、TypeScript、Element Plus。

## Global Constraints

- `PLATFORM_ADMIN` 固定全量只读。
- 健身房管理员只能配置当前门店的 `COACH`、`MEMBER`。
- 不删除、清空、重置或重建已有数据库数据。
- 只提交本需求相关文件。

### Task 1: 门店覆盖模型和权限解析

- [ ] 新增门店角色权限实体、仓储和 Spring Data 实现。
- [ ] 让登录、刷新、PrincipalLoader、邀请接受统一按门店覆盖解析权限。
- [ ] 添加覆盖优先、空覆盖和租户隔离测试。

### Task 2: 角色权限服务和 API

- [ ] 为角色目录和保存操作增加作用域判断。
- [ ] 平台管理员写入全局模板，健身房管理员写入当前门店覆盖。
- [ ] 添加服务及 MockMvc 权限测试。

### Task 3: 菜单目录与前端页面

- [ ] 将角色权限菜单开放给健身房管理员。
- [ ] 页面按作用域展示可编辑角色并区分只读角色。
- [ ] 添加前端类型检查和页面行为测试。

### Task 4: Schema、验证和提交

- [ ] 更新 schema contract 与生产增量 SQL 文档。
- [ ] 运行后端聚焦测试、Maven 测试和前端构建。
- [ ] 只提交本任务文件，确认剩余工作区改动未被纳入提交。
