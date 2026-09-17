# GymMind 项目 AI 协作规范

> **重要**: 本文档是 AI 助手在修改代码时必须遵循的规范。每次修改前请仔细阅读。

## 📋 核心原则

### 1. 代码注释规范 ✅ 强制执行

**所有代码必须使用中文注释**

#### Java 代码注释
```java
/**
 * 用户服务类
 * 处理用户注册、登录、个人信息管理等功能
 * 
 * @author GymMind Team
 */
@Service
public class UserService {
    
    /**
     * 用户注册
     * 
     * @param request 注册请求对象，包含用户名、邮箱、密码
     * @return 注册成功后的用户信息
     * @throws UserExistsException 当用户名或邮箱已存在时抛出
     */
    public UserDTO register(RegisterRequest request) {
        // 1. 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserExistsException("用户名已存在");
        }
        
        // 2. 加密密码
        String encryptedPassword = passwordEncoder.encode(request.getPassword());
        
        // 3. 创建用户实体
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(encryptedPassword);
        
        // 4. 保存到数据库
        return userRepository.save(user);
    }
}
```

#### TypeScript/JavaScript 代码注释
```typescript
/**
 * 用户认证 Store
 * 管理用户登录状态、Token 存储等
 */
export const useAuthStore = defineStore('auth', () => {
  // 用户 Token（存储在 localStorage）
  const token = ref<string | null>(localStorage.getItem('token'))
  
  // 用户 ID
  const userId = ref<number | null>(null)
  
  /**
   * 用户登录
   * 
   * @param credentials 登录凭证（用户名和密码）
   * @returns Promise<void>
   */
  async function login(credentials: LoginRequest): Promise<void> {
    try {
      // 1. 调用登录 API
      const response = await authApi.login(credentials)
      
      // 2. 保存 Token 到本地存储
      token.value = response.data.token
      localStorage.setItem('token', response.data.token)
      
      // 3. 跳转到仪表板
      router.push('/dashboard')
    } catch (error) {
      // 登录失败，显示错误消息
      ElMessage.error('登录失败，请检查用户名和密码')
      throw error
    }
  }
  
  return { token, userId, login }
})
```

#### Vue 组件注释
```vue
<template>
  <!-- 登录页面容器 -->
  <div class="auth-page">
    <!-- 背景大图 -->
    <div class="hero-backdrop">
      <img src="..." alt="健身训练" />
    </div>
    
    <!-- 登录表单卡片 -->
    <div class="auth-card">
      <!-- 表单标题 -->
      <header class="auth-header">
        <h1>欢迎回到 <em>GymMind</em></h1>
      </header>
      
      <!-- 登录表单 -->
      <form @submit.prevent="handleLogin">
        <!-- ... -->
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 登录页面组件
 * 
 * 功能：
 * - 用户名密码登录
 * - 表单验证
 * - 错误提示
 */

// 用户名（双向绑定）
const username = ref('')

// 密码（双向绑定）
const password = ref('')

// 是否显示密码
const showPassword = ref(false)

// 是否正在加载
const loading = ref(false)
</script>
```

---

### 2. Git 提交规范 ✅ 强制执行

**每次完成代码修改后，必须提交到远程仓库**

#### 提交流程（自动化）
```bash
# 1. 查看修改状态
git status

# 2. 添加修改的文件（不要用 git add .）
git add <具体文件路径>

# 3. 提交（中文 commit message）
git commit -m "feat: 添加用户登录功能

- 实现用户名密码登录
- 添加 JWT Token 验证
- 完善错误提示"

# 4. 推送到远程仓库
git push origin main
```

#### Commit Message 规范

**格式**: `<类型>: <简短描述>`

**类型列表**:
- `feat`: 新功能
- `fix`: 修复 Bug
- `docs`: 文档更新
- `style`: 代码格式（不影响功能）
- `refactor`: 重构（不是新增功能，也不是修复 Bug）
- `perf`: 性能优化
- `test`: 添加测试
- `chore`: 构建过程或辅助工具的变动

**示例**:
```bash
# 好的示例 ✅
git commit -m "feat: 添加 AI 助手对话功能"
git commit -m "fix: 修复登录页面密码显示问题"
git commit -m "docs: 更新 README 文档"
git commit -m "refactor: 重构仪表板组件结构"

# 不好的示例 ❌
git commit -m "update"
git commit -m "修改了一些东西"
git commit -m "fix bug"
```

---

### 3. AI 修改代码工作流程 ✅ 必须遵循

#### 每次修改代码时的步骤：

**步骤 1: 理解需求**
- 仔细阅读用户的需求
- 明确要修改的文件和功能
- 列出需要完成的任务

**步骤 2: 修改代码**
- 使用 Read 工具读取现有代码
- 使用 Edit/Write 工具修改代码
- **添加中文注释**（强制）
- 保持代码整洁和一致性

**步骤 3: 验证修改**
- 检查语法错误
- 确保逻辑正确
- 测试关键功能

**步骤 4: Git 提交**（强制）
```bash
# 查看修改
git status

# 添加文件
git add <修改的文件>

# 提交（中文消息）
git commit -m "feat: <功能描述>

- <详细说明1>
- <详细说明2>"

# 推送
git push origin main
```

**步骤 5: 向用户报告**
- 总结完成的修改
- 说明关键变更点
- 提供测试建议

---

## 📁 文件命名规范

### 前端文件
```
✅ 好的命名:
- LoginView.vue          (页面组件，PascalCase)
- useAuthStore.ts        (Composable，camelCase with use前缀)
- auth.service.ts        (服务类，camelCase.type)
- premium.css            (样式文件，kebab-case)

❌ 不好的命名:
- login.vue
- authStore.ts
- AuthService.ts
- Premium.css
```

### 后端文件
```
✅ 好的命名:
- UserService.java       (服务类，PascalCase)
- UserController.java    (控制器，PascalCase)
- UserRepository.java    (Repository，PascalCase)
- user_schema.sql        (SQL 文件，snake_case)

❌ 不好的命名:
- userService.java
- user-controller.java
- UserRepo.java
```

---

## 🎨 代码风格规范

### Java 代码风格
```java
// ✅ 好的代码风格
@Service
public class WorkoutService {
    
    private final WorkoutRepository workoutRepository;
    private final UserRepository userRepository;
    
    /**
     * 构造函数（依赖注入）
     */
    public WorkoutService(
        WorkoutRepository workoutRepository,
        UserRepository userRepository
    ) {
        this.workoutRepository = workoutRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * 创建训练记录
     */
    public WorkoutDTO createWorkout(CreateWorkoutRequest request) {
        // 1. 验证用户存在
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new UserNotFoundException("用户不存在"));
        
        // 2. 创建训练记录
        Workout workout = new Workout();
        workout.setUser(user);
        workout.setExerciseName(request.getExerciseName());
        workout.setSets(request.getSets());
        workout.setReps(request.getReps());
        
        // 3. 保存到数据库
        Workout saved = workoutRepository.save(workout);
        
        // 4. 转换为 DTO 返回
        return WorkoutMapper.toDTO(saved);
    }
}
```

### TypeScript/Vue 代码风格
```typescript
// ✅ 好的代码风格

/**
 * 仪表板页面
 */
export default defineComponent({
  name: 'DashboardView',
  
  setup() {
    // ===== 状态定义 =====
    
    // 是否正在加载
    const loading = ref(true)
    
    // 仪表板数据
    const summary = ref<DashboardSummary | null>(null)
    
    // ===== 计算属性 =====
    
    // 卡路里摄入百分比
    const caloriePercentage = computed(() => {
      if (!summary.value) return 0
      const { caloriesConsumed, caloriesTarget } = summary.value.todayOverview
      return Math.min(Math.round((caloriesConsumed / caloriesTarget) * 100), 100)
    })
    
    // ===== 方法定义 =====
    
    /**
     * 加载仪表板数据
     */
    async function loadDashboard() {
      try {
        loading.value = true
        const data = await getDashboardSummary()
        summary.value = data
      } catch (error) {
        console.error('加载仪表板失败:', error)
        ElMessage.error('加载数据失败，请刷新页面')
      } finally {
        loading.value = false
      }
    }
    
    // ===== 生命周期 =====
    
    onMounted(() => {
      loadDashboard()
    })
    
    return {
      loading,
      summary,
      caloriePercentage,
      loadDashboard
    }
  }
})
```

---

## 🚫 禁止事项

### 绝对不要做的事情：

1. ❌ **不要使用英文注释**
   - 所有注释必须用中文
   
2. ❌ **不要跳过 Git 提交**
   - 每次修改代码后必须提交
   
3. ❌ **不要使用 `git add .`**
   - 必须明确指定要添加的文件
   
4. ❌ **不要写模糊的 Commit Message**
   - "update" / "fix" / "修改" 都不可接受
   
5. ❌ **不要删除现有的中文注释**
   - 如果修改代码，同步更新注释
   
6. ❌ **不要直接 `git push --force`**
   - 除非得到明确授权

---

## ✅ AI 完成任务的检查清单

每次完成任务后，AI 必须确认：

- [ ] 所有新增/修改的代码都有中文注释
- [ ] 关键逻辑都有解释性注释
- [ ] 已执行 `git add <文件>`
- [ ] 已执行 `git commit -m "类型: 描述"`
- [ ] 已执行 `git push origin main`
- [ ] Commit message 清晰描述了改动
- [ ] 代码格式符合规范
- [ ] 没有遗留 TODO 或 FIXME
- [ ] 向用户报告了完成情况

---

## 📝 示例：完整的代码修改流程

### 场景：添加"忘记密码"功能

**1. 后端代码 (UserService.java)**
```java
/**
 * 发送密码重置邮件
 * 
 * @param email 用户邮箱
 * @throws UserNotFoundException 用户不存在
 */
public void sendPasswordResetEmail(String email) {
    // 1. 查找用户
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new UserNotFoundException("邮箱未注册"));
    
    // 2. 生成重置 Token（6位数字）
    String resetToken = String.valueOf((int)(Math.random() * 900000) + 100000);
    
    // 3. 设置 Token 过期时间（30分钟）
    user.setResetToken(resetToken);
    user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
    userRepository.save(user);
    
    // 4. 发送邮件
    emailService.sendResetPasswordEmail(email, resetToken);
}
```

**2. 前端代码 (ForgotPasswordView.vue)**
```vue
<script setup lang="ts">
/**
 * 忘记密码页面
 * 
 * 功能：
 * - 输入邮箱
 * - 发送重置邮件
 * - 显示成功提示
 */

// 邮箱地址
const email = ref('')

// 是否正在发送
const loading = ref(false)

/**
 * 发送重置邮件
 */
async function handleSendEmail() {
  // 验证邮箱格式
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) {
    ElMessage.error('请输入有效的邮箱地址')
    return
  }
  
  loading.value = true
  try {
    // 调用 API
    await authApi.sendPasswordResetEmail(email.value)
    
    // 显示成功提示
    ElMessage.success('重置邮件已发送，请查收')
    
    // 3秒后跳转到登录页
    setTimeout(() => {
      router.push('/login')
    }, 3000)
  } catch (error) {
    ElMessage.error('发送失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>
```

**3. Git 提交**
```bash
# 添加修改的文件
git add backend/src/main/java/com/gymmind/service/UserService.java
git add frontend/src/views/ForgotPasswordView.vue
git add frontend/src/api/auth.ts

# 提交
git commit -m "feat: 添加忘记密码功能

- 后端：实现密码重置邮件发送
- 前端：添加忘记密码页面
- 生成6位数字重置码，有效期30分钟"

# 推送
git push origin main
```

**4. 向用户报告**
```
✅ 已完成"忘记密码"功能

修改文件：
- backend/src/main/java/com/gymmind/service/UserService.java
- frontend/src/views/ForgotPasswordView.vue  
- frontend/src/api/auth.ts

功能说明：
1. 用户输入邮箱后，系统发送6位数字重置码
2. 重置码有效期30分钟
3. 前端提供友好的表单和反馈

已提交到远程仓库 ✅
Commit: feat: 添加忘记密码功能
```

---

## 🔄 持续改进

本规范会根据项目发展不断更新。如果发现问题或有改进建议，请及时更新此文档。

**最后更新**: 2026-09-11
**维护者**: GymMind Team
