# 系统测试指南

## 测试范围

本文档涵盖GymMind系统的功能测试、安全测试和性能验证。

---

## 1. 功能测试

### 1.1 用户认证测试

**注册功能**
```bash
# 测试用户注册
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser1",
    "password": "Test123456",
    "email": "test1@example.com"
  }'

# 预期结果：返回success，包含用户信息
```

**登录功能**
```bash
# 测试用户登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser1",
    "password": "Test123456"
  }'

# 预期结果：返回JWT token
# 保存token供后续请求使用
export TOKEN="返回的token值"
```

**JWT验证**
```bash
# 测试无token访问受保护接口
curl http://localhost:8080/api/user/profile

# 预期结果：401 Unauthorized

# 测试有效token访问
curl http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer $TOKEN"

# 预期结果：200 OK，返回用户档案
```

---

### 1.2 用户档案测试

**创建档案**
```bash
curl -X PUT http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "age": 25,
    "gender": "MALE",
    "height": 175.0,
    "weight": 70.0,
    "fitnessGoal": "MUSCLE_GAIN",
    "experienceLevel": "INTERMEDIATE",
    "trainingFrequency": 4,
    "availableEquipment": ["BARBELL", "DUMBBELL"],
    "dietaryPreference": "BALANCED",
    "allergies": "无"
  }'

# 预期结果：档案创建成功
```

---

### 1.3 知识库测试

**上传文档**
```bash
# 准备测试PDF文件
curl -X POST http://localhost:8080/api/knowledge/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@test_fitness.pdf" \
  -F "category=FITNESS_GUIDE"

# 预期结果：返回文档ID，状态为PENDING
```

**查询文档列表**
```bash
curl "http://localhost:8080/api/knowledge?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"

# 预期结果：返回当前用户的文档列表
```

**查询文档详情**
```bash
curl http://localhost:8080/api/knowledge/{documentId} \
  -H "Authorization: Bearer $TOKEN"

# 预期结果：返回文档详情，包含处理状态
```

---

### 1.4 RAG问答测试

**发送问题**
```bash
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "如何进行增肌训练？",
    "history": []
  }'

# 预期结果：返回AI回答，包含来源引用
```

**验证混合检索**
- 检查返回的sources字段
- 验证同时包含向量检索和关键词检索的结果
- 确认RRF融合排序生效

---

### 1.5 训练记录测试

**创建训练记录**
```bash
curl -X POST http://localhost:8080/api/workout/record \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "exerciseName": "Bench Press",
    "sets": 4,
    "reps": 10,
    "weight": 80.0,
    "rpe": 8,
    "workoutDate": "2026-09-10"
  }'

# 预期结果：记录创建成功
```

**查询统计数据**
```bash
curl "http://localhost:8080/api/workout/statistics?startDate=2026-09-01&endDate=2026-09-10" \
  -H "Authorization: Bearer $TOKEN"

# 预期结果：返回训练统计
```

---

### 1.6 营养记录测试

**营养计算**
```bash
curl -X POST http://localhost:8080/api/nutrition/calculate \
  -H "Authorization: Bearer $TOKEN"

# 预期结果：返回BMI、BMR、TDEE和宏量营养素目标
```

**创建饮食记录**
```bash
curl -X POST http://localhost:8080/api/nutrition/record \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "mealType": "BREAKFAST",
    "foodName": "鸡胸肉",
    "calories": 200,
    "protein": 40,
    "carbs": 0,
    "fats": 4,
    "recordDate": "2026-09-10"
  }'

# 预期结果：记录创建成功
```

---

### 1.7 周报生成测试

**生成周报**
```bash
curl "http://localhost:8080/api/report/weekly?startDate=2026-09-01&endDate=2026-09-07" \
  -H "Authorization: Bearer $TOKEN"

# 预期结果：返回包含AI分析的周报
```

---

## 2. 安全测试

### 2.1 数据隔离测试

**测试跨用户访问**
```bash
# 创建第二个用户
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser2",
    "password": "Test123456",
    "email": "test2@example.com"
  }'

# 获取第二个用户的token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser2",
    "password": "Test123456"
  }'

export TOKEN2="第二个用户的token"

# 用户2上传文档
curl -X POST http://localhost:8080/api/knowledge/upload \
  -H "Authorization: Bearer $TOKEN2" \
  -F "file=@test2.pdf"

# 用户1尝试访问用户2的文档列表
curl "http://localhost:8080/api/knowledge" \
  -H "Authorization: Bearer $TOKEN"

# 预期结果：只能看到用户1自己的文档，看不到用户2的文档
```

**自动化隔离测试**
```bash
curl http://localhost:8080/api/system/test/isolation \
  -H "Authorization: Bearer $TOKEN"

# 预期结果：所有隔离测试PASS
```

---

### 2.2 密码安全测试

**验证密码加密**
- 检查数据库中的密码字段
- 确认使用BCrypt加密（$2a$或$2b$前缀）

```sql
-- 在MySQL中查询
SELECT id, username, password FROM users;

-- 预期：password字段应为加密字符串，不是明文
```

**密码复杂度测试**
```bash
# 测试弱密码
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "weakuser",
    "password": "123",
    "email": "weak@example.com"
  }'

# 预期结果：返回错误，密码不符合要求
```

---

### 2.3 文件上传安全测试

**测试文件类型限制**
```bash
# 尝试上传不支持的文件类型
curl -X POST http://localhost:8080/api/knowledge/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@malicious.exe"

# 预期结果：返回错误，文件类型不支持
```

**测试文件大小限制**
```bash
# 创建超大文件（>50MB）
dd if=/dev/zero of=large.pdf bs=1M count=60

# 尝试上传
curl -X POST http://localhost:8080/api/knowledge/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@large.pdf"

# 预期结果：返回错误，文件过大
```

---

## 3. 性能测试

### 3.1 RAG检索性能

**单次检索耗时**
```bash
# 测试RAG响应时间
time curl -X POST http://localhost:8080/api/ai/chat \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message": "增肌训练计划", "history": []}'

# 预期结果：响应时间 < 5秒
```

**并发检索测试**
```bash
# 使用Apache Bench进行并发测试
ab -n 10 -c 2 -H "Authorization: Bearer $TOKEN" \
  -p chat_request.json -T application/json \
  http://localhost:8080/api/ai/chat

# 预期结果：所有请求成功，平均响应时间可接受
```

---

### 3.2 文档处理性能

**批量上传测试**
```bash
# 上传多个文档并监控处理时间
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/knowledge/upload \
    -H "Authorization: Bearer $TOKEN" \
    -F "file=@test_doc_$i.pdf" &
done
wait

# 监控处理状态
watch -n 2 'curl -s http://localhost:8080/api/knowledge -H "Authorization: Bearer $TOKEN" | jq'
```

---

## 4. 系统健康检查

### 4.1 服务健康状态

```bash
curl http://localhost:8080/api/system/health \
  -H "Authorization: Bearer $TOKEN"

# 预期结果：所有服务状态HEALTHY
```

### 4.2 系统统计

```bash
curl http://localhost:8080/api/system/stats \
  -H "Authorization: Bearer $TOKEN"

# 返回系统统计信息
```

---

## 5. 集成测试场景

### 场景1：完整用户流程

1. 注册新用户
2. 登录获取token
3. 完善用户档案
4. 上传健身文档
5. 等待文档处理完成
6. 进行RAG问答
7. 记录训练数据
8. 记录营养数据
9. 生成周报

### 场景2：数据一致性验证

1. 创建训练记录
2. 查询Dashboard统计
3. 验证数据一致性
4. 生成周报
5. 验证周报数据与实际记录匹配

---

## 6. 问题排查

### 常见问题

**问题1：文档处理失败**
```bash
# 查看失败原因
curl http://localhost:8080/api/knowledge/{docId} \
  -H "Authorization: Bearer $TOKEN"

# 重试处理
curl -X POST http://localhost:8080/api/knowledge/{docId}/retry \
  -H "Authorization: Bearer $TOKEN"
```

**问题2：RAG返回无结果**
- 检查文档是否处理成功
- 验证Milvus和ES连接
- 查看后端日志

**问题3：JWT过期**
```bash
# 重新登录获取新token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser1", "password": "Test123456"}'
```

---

## 7. 测试报告

### 测试结果记录模板

| 测试项 | 状态 | 备注 |
|---|---|---|
| 用户注册 | ✅ PASS | |
| 用户登录 | ✅ PASS | |
| JWT验证 | ✅ PASS | |
| 档案管理 | ✅ PASS | |
| 文档上传 | ✅ PASS | |
| 文档处理 | ✅ PASS | |
| RAG问答 | ✅ PASS | |
| 混合检索 | ✅ PASS | |
| 训练记录 | ✅ PASS | |
| 营养计算 | ✅ PASS | |
| 周报生成 | ✅ PASS | |
| 数据隔离 | ✅ PASS | |
| 密码安全 | ✅ PASS | |
| 文件安全 | ✅ PASS | |
| 性能测试 | ✅ PASS | |

---

## 8. 自动化测试脚本

参考 `scripts/test-all.sh` 执行完整测试套件。

```bash
# 运行全部测试
./scripts/test-all.sh

# 运行特定测试
./scripts/test-all.sh --only-security
./scripts/test-all.sh --only-api
```
