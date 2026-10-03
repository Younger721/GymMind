# 鹈鹕骑自行车 SVG 动画 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 创建一个可直接打开的单文件 SVG 动画页面，展示鹈鹕骑自行车并提供暂停与 AI 状态切换交互。

**Architecture:** 所有资源集中在 `output/pelican-bike.html`。内联 SVG 负责场景与角色，CSS keyframes 负责持续运动，少量 JavaScript 负责按钮事件和状态数组。

**Tech Stack:** HTML5、SVG、CSS、原生 JavaScript。

## Global Constraints

- 页面不得依赖外部网络资源或构建步骤。
- 不修改当前工作区已有未提交文件。
- 页面文案和交互以中文呈现。

### Task 1: 创建单文件动画页面

**Files:**
- Create: `output/pelican-bike.html`

**Interfaces:**
- DOM controls: `#pauseButton` pauses/resumes `.motion` elements；`#statusButton` cycles `statusItems`。
- SVG groups: `.pelican`, `.bike`, `.cloud`, `.road-mark` receive CSS animations.

- [ ] **Step 1: 写入 HTML 骨架、内联样式和 SVG 场景**
  - 添加语义化标题、控制按钮、状态徽章。
  - 通过内联 SVG 绘制背景、云、道路、自行车轮组、车架、鹈鹕身体/头部/喙/翅膀/腿。
  - 用 CSS `@keyframes` 实现 `wheel-spin`、`pedal-spin`、`bob`、`cloud-drift`、`road-slide`。

- [ ] **Step 2: 添加交互脚本**
  - 维护 `isPaused` 和 `statusIndex`。
  - 暂停按钮给页面根节点切换 `is-paused` 类，并更新 `aria-pressed` 与文案。
  - 状态按钮循环更新徽章文字、颜色类和进度条宽度。

- [ ] **Step 3: 在浏览器中验证**
  - 启动本地静态服务并打开文件。
  - 验证 SVG 非空、动画可见、两个按钮可用、窄屏没有横向溢出。

- [ ] **Step 4: 提交变更**
  - `git diff -- output/pelican-bike.html docs/superpowers/specs docs/superpowers/plans`
  - `git add output/pelican-bike.html docs/superpowers/specs/2026-10-03-pelican-bike-animation-design.md docs/superpowers/plans/2026-10-03-pelican-bike-animation.md`
  - `git commit -m "feat: 新增鹈鹕骑自行车 SVG 动画"`
