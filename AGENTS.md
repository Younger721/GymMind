# GymMind 协作规范

## 本地后端启动（AI 助手）

在仓库根目录启动后端时**优先**使用固定脚本（会先加载根 `.env` 再 `spring-boot:run`）：

```bat
start-backend-dev.bat
```

不要裸跑 `mvn spring-boot:run`（除非已 `call scripts\load-root-env.bat`）。全栈含前端时用 `start-all.bat`。IDEA 用户使用共享运行配置 **GymMindApplication (dev)**（见 `.run/GymMindApplication_dev.run.xml`）。

## 数据库删除操作确认

凡是可能删除数据库数据、数据表、数据库，或执行清空、重置、重建等会导致已有数据丢失的操作，必须先向用户说明具体操作、影响范围和目标环境，并获得用户明确同意后才能执行。未获得明确同意时，只允许进行只读查询、备份和风险分析，不得执行相关修改或删除操作。

## Git 提交要求

每次修改代码、配置、测试或文档后，都必须提交到本地 Git 仓库。提交前应检查改动范围，避免把无关文件混入提交。

提交信息必须使用以下格式：

```text
<类型>: <中文说明>
```

类型使用以下前缀：

- `feat`: 新功能
- `fix`: 修复问题
- `docs`: 文档修改
- `style`: 格式或样式调整
- `refactor`: 重构
- `perf`: 性能优化
- `test`: 测试修改
- `build`: 构建相关修改
- `ci`: 持续集成修改
- `chore`: 构建过程或辅助工具修改
- `revert`: 回滚
- `workflow`: 工作流改进
- `mod`: 不确定分类的修改
- `wip`: 开发中
- `types`: 类型修改

示例：

```text
feat: 新增会员风险分析功能
fix: 修复课程预约状态更新问题
docs: 补充开发环境说明
test: 增加用户登录测试
```

提交前执行：

```bash
git status
git diff
git add -A
git commit -m "类型: 中文说明"
git status
```

提交完成后确认工作区没有遗漏的未提交文件。若工作区中存在其他对话或其他任务产生的改动，只提交当前任务相关文件，并在结果中说明剩余未提交内容。
