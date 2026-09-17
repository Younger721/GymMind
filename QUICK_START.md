# GymMind 项目启动指南 🚀

## 📋 前置准备检查清单

在启动项目前，请确保以下工具已安装：

- [ ] Java 17+
- [ ] Maven 3.8+
- [ ] Node.js 18+
- [ ] Docker & Docker Compose
- [ ] MySQL 8.0
- [ ] Git

---

## 🔧 第一步：环境配置

### 1.1 启动基础设施服务

```bash
cd D:\java\project\full-project\GymMind
docker-compose up -d
```

这会启动以下服务：
- MySQL (端口 3306)
- Redis (端口 6379)
- Milvus (端口 19530)
- Elasticsearch (端口 9200)
- MinIO (端口 9000)

### 1.2 验证服务状态

```bash
docker-compose ps
```

确保所有服务状态为 `Up`。

### 1.3 创建数据库表

1. 连接到 MySQL：
```bash
mysql -h 127.0.0.1 -u root -p
# 密码: 你的MySQL密码
```

2. 创建数据库（如果不存在）：
```sql
CREATE DATABASE IF NOT EXISTS gymmind CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE gymmind;
```

3. 执行建表脚本：
```bash
mysql -h 127.0.0.1 -u root -p gymmind < D:\java\project\full-project\GymMind\docs\database_schema.sql
```

或者在 MySQL 客户端中直接执行 `docs/database_schema.sql` 文件内容。

### 1.4 配置后端环境变量

编辑 `backend/src/main/resources/application.yml` 或创建 `application-local.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/gymmind?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 你的MySQL密码
    
  data:
    redis:
      host: localhost
      port: 6379
      password: # 如果有密码就填写
      
ai:
  openai:
    api-key: sk-你的OpenAI-API-Key
    base-url: https://api.openai.com/v1
    chat:
      model: gpt-4o
      
milvus:
  host: localhost
  port: 19530
  
elasticsearch:
  host: localhost
  port: 9200
  
minio:
  endpoint: http://localhost:9000
  accessKey: minioadmin
  secretKey: minioadmin
  bucketName: gymmind
```

---

## 🚀 第二步：启动后端

### 2.1 安装依赖并编译

```bash
cd D:\java\project\full-project\GymMind\backend
mvn clean install -DskipTests
```

### 2.2 启动后端服务

```bash
mvn spring-boot:run
```

或者在 IDE 中运行 `GymMindApplication.java`。

### 2.3 验证后端启动成功

访问：http://localhost:8080/actuator/health

应该返回：
```json
{
  "status": "UP"
}
```

---

## 🎨 第三步：启动前端

### 3.1 安装依赖

```bash
cd D:\java\project\full-project\GymMind\frontend
npm install
```

这会安装所有依赖，包括新添加的：
- sockjs-client
- @stomp/stompjs

### 3.2 启动开发服务器

```bash
npm run dev
```

### 3.3 访问前端

浏览器打开：http://localhost:5173

---

## 🧪 第四步：功能测试

### 4.1 注册登录

1. 访问 http://localhost:5173
2. 点击"注册"
3. 填写用户信息并注册
4. 使用注册的账号登录

### 4.2 测试新功能

#### 智能训练计划生成器
- URL: http://localhost:5173/plan-generator
- 测试步骤：
  1. 填写训练目标（增肌/减脂/力量/耐力）
  2. 设置训练周数（4/8/12周）
  3. 选择训练频率（3-6天/周）
  4. 点击"生成计划"
  5. 查看 AI 生成的详细计划

#### 数据可视化仪表盘
- URL: http://localhost:5173/dashboard
- 查看 8 种图表：
  - 统计卡片
  - 体重趋势图
  - 训练分布饼图
  - 训练热力图
  - 肌群雷达图
  - 营养趋势图

#### 社交动态广场
- URL: http://localhost:5173/social
- 测试步骤：
  1. 发布一条动态
  2. 点赞动态
  3. 评论动态
  4. 切换"最新/热门/我的"标签

#### 健身挑战系统
- URL: http://localhost:5173/challenges
- 测试步骤：
  1. 创建一个挑战
  2. 加入挑战
  3. 更新进度
  4. 查看排行榜

---

## 🐛 常见问题排查

### 问题 1: 后端启动失败

**原因**: 数据库连接失败

**解决方案**:
```bash
# 检查 MySQL 是否运行
docker ps | grep mysql

# 检查数据库配置
# 确保 application.yml 中的数据库密码正确
```

### 问题 2: 前端无法连接后端

**原因**: CORS 跨域问题

**解决方案**:
检查 `backend/src/main/java/com/gymmind/config/WebConfig.java` 是否配置了 CORS：

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true);
    }
}
```

### 问题 3: WebSocket 连接失败

**原因**: WebSocket 配置问题

**解决方案**:
1. 检查后端 WebSocket 端点是否正确：`ws://localhost:8080/ws`
2. 检查前端 `utils/websocket.ts` 中的连接地址
3. 查看浏览器控制台是否有 WebSocket 错误

### 问题 4: AI 功能不工作

**原因**: OpenAI API Key 未配置或无效

**解决方案**:
1. 确保 `application.yml` 中配置了有效的 API Key
2. 检查 API Key 是否有足够的额度
3. 尝试访问 OpenAI API 测试连接

### 问题 5: 图片上传失败

**原因**: MinIO 未启动或配置错误

**解决方案**:
```bash
# 检查 MinIO 是否运行
docker ps | grep minio

# 访问 MinIO 控制台
http://localhost:9000
# 用户名: minioadmin
# 密码: minioadmin

# 创建 bucket: gymmind
```

---

## 📊 项目端口汇总

| 服务 | 端口 | 访问地址 |
|------|------|---------|
| 前端 | 5173 | http://localhost:5173 |
| 后端 API | 8080 | http://localhost:8080 |
| MySQL | 3306 | localhost:3306 |
| Redis | 6379 | localhost:6379 |
| Milvus | 19530 | localhost:19530 |
| Elasticsearch | 9200 | http://localhost:9200 |
| MinIO | 9000 | http://localhost:9000 |

---

## 🎯 API 测试

### 使用 curl 测试

```bash
# 1. 注册用户
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
  }'

# 2. 登录获取 Token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'

# 保存返回的 token
TOKEN="返回的JWT Token"

# 3. 生成训练计划
curl -X POST http://localhost:8080/api/workout-plans/generate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "goal": "MUSCLE_GAIN",
    "durationWeeks": 8,
    "workoutsPerWeek": 4,
    "difficulty": "INTERMEDIATE"
  }'

# 4. 创建动态
curl -X POST http://localhost:8080/api/social/posts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "今天完成了胸部训练！",
    "postType": "WORKOUT"
  }'

# 5. 获取动态流
curl -X GET "http://localhost:8080/api/social/posts/feed?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

---

## ✅ 启动成功标志

当你看到以下内容时，说明项目启动成功：

### 后端控制台
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.5)

Started GymMindApplication in 15.234 seconds
```

### 前端控制台
```
  VITE v5.2.0  ready in 1234 ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
```

### 浏览器
- 能正常访问登录页面
- 登录后能看到仪表盘
- 所有新功能页面都能访问

---

## 🎓 下一步

项目启动成功后，你可以：

1. **体验所有新功能** - 按照上面的测试步骤逐一体验
2. **查看文档** - 阅读 `README_NEW_FEATURES.md` 了解详细功能
3. **开发新功能** - 基于现有架构添加新的功能
4. **准备答辩** - 整理项目亮点和技术创新点

---

## 📞 需要帮助？

如果遇到任何问题：
1. 检查上面的"常见问题排查"部分
2. 查看控制台错误日志
3. 检查 `backend/logs/` 目录下的日志文件

祝你项目运行顺利！🎉
