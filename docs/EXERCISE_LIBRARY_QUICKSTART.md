# 动作库功能快速使用指南

## ✅ 已完成功能

### 1. 后端功能
- ✅ 动作库实体类 (ExerciseLibrary)
- ✅ 用户收藏实体类 (UserFavoriteExercise)
- ✅ 数据访问层 (Repository)
- ✅ 业务逻辑层 (Service)
- ✅ REST API 控制器 (Controller)
- ✅ 支持分页、筛选、搜索、排序
- ✅ 支持收藏/取消收藏功能
- ✅ 支持热门和推荐动作查询

### 2. 前端功能
- ✅ 动作库展示页面
- ✅ 视频网格布局
- ✅ 筛选器（类别、难度、器械、排序）
- ✅ 搜索功能
- ✅ 视频详情弹窗
- ✅ 视频播放器
- ✅ 收藏功能
- ✅ 分页功能

### 3. 视频爬虫工具
- ✅ Python爬虫脚本
- ✅ 支持抖音和TikTok平台
- ✅ 自动生成缩略图
- ✅ 自动提取视频信息
- ✅ 批量下载功能
- ✅ 详细使用教程

## 🚀 快速启动

### 步骤1：创建数据库表

```bash
# 连接到MySQL数据库
mysql -u root -p gymmind

# 执行建表脚本
source docs/create_exercise_library.sql
```

或者直接在MySQL客户端中执行 `docs/create_exercise_library.sql` 文件。

### 步骤2：启动后端服务

```bash
cd backend
mvn spring-boot:run
```

后端会在 `http://localhost:8080` 启动。

### 步骤3：启动前端服务

```bash
cd frontend
npm install  # 如果还没安装依赖
npm run dev
```

前端会在 `http://localhost:5173` 启动。

### 步骤4：访问动作库

打开浏览器访问：`http://localhost:5173`

1. 先登录账号
2. 在左侧导航栏点击「动作库」
3. 浏览、搜索、筛选动作
4. 点击动作卡片查看详情和视频

## 📹 使用视频爬虫

### 安装依赖

```bash
# 安装Python依赖
pip install requests yt-dlp

# 安装FFmpeg（用于生成缩略图）
# Windows: 下载并添加到PATH
# Mac: brew install ffmpeg
# Linux: sudo apt-get install ffmpeg
```

### 运行爬虫

```bash
# 交互式运行
python scripts/video_scraper.py

# 按提示操作：
# 1. 下载单个视频
# 2. 批量下载视频
# 3. 查看下载记录
# 4. 退出
```

### 示例：下载健身视频

```
请输入视频URL: https://www.douyin.com/video/1234567890
输入文件名: bench-press

🔍 正在解析抖音视频...
✅ 视频下载成功: ./downloaded_videos/bench-press.mp4
✅ 缩略图生成成功: ./thumbnails/bench-press.jpg
```

下载的文件会保存在：
- 视频：`downloaded_videos/`
- 缩略图：`thumbnails/`
- 元数据：`video_metadata.json`

## 📊 API接口说明

### 查询动作列表
```
GET /api/exercises?category=CHEST&difficulty=INTERMEDIATE&page=0&size=12
```

### 查询动作详情
```
GET /api/exercises/{id}
```

### 收藏/取消收藏
```
POST /api/exercises/{id}/favorite
```

### 查询用户收藏
```
GET /api/exercises/favorites
```

### 查询热门动作
```
GET /api/exercises/popular
```

### 查询推荐动作
```
GET /api/exercises/recommended
```

## 🎯 数据库中的示例数据

建表脚本中已包含5条示例数据：
1. 杠铃卧推 (Barbell Bench Press)
2. 引体向上 (Pull-up)
3. 深蹲 (Barbell Squat)
4. 哑铃肩推 (Dumbbell Shoulder Press)
5. 平板支撑 (Plank)

你可以直接在前端查看这些示例动作。

## 📝 添加新动作

### 方法1：通过API添加

```bash
curl -X POST http://localhost:8080/api/exercises \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "杠铃硬拉",
    "nameEn": "Barbell Deadlift",
    "category": "BACK",
    "difficulty": "ADVANCED",
    "equipment": "BARBELL",
    "videoUrl": "/videos/deadlift.mp4",
    "videoThumbnail": "/thumbnails/deadlift.jpg",
    "videoDuration": 60,
    "videoSource": "UPLOAD",
    "description": "力量训练之王",
    "targetMuscles": "背部、臀部、腿部",
    "instructions": ["双脚与肩同宽", "握住杠铃", "保持背部挺直拉起"],
    "tips": ["注意腰部保护", "循序渐进增加重量"]
  }'
```

### 方法2：直接插入数据库

```sql
INSERT INTO exercise_library 
(name, name_en, category, difficulty, equipment, video_url, video_thumbnail, description, target_muscles, status, is_verified)
VALUES
('杠铃硬拉', 'Barbell Deadlift', 'BACK', 'ADVANCED', 'BARBELL', 
 '/videos/deadlift.mp4', '/thumbnails/deadlift.jpg', 
 '力量训练之王', '背部、臀部、腿部', 'ACTIVE', TRUE);
```

## 🔧 自定义配置

### 修改视频存储路径

编辑 `backend/src/main/resources/application.yml`:

```yaml
video:
  storage-path: /path/to/videos
  thumbnail-path: /path/to/thumbnails
```

### 修改分页大小

前端默认每页显示12个动作，可以在 `ExerciseLibraryView.vue` 中修改：

```typescript
const pageSize = ref(12)  // 改为你想要的数量
```

## 📖 详细文档

- **视频爬虫完整教程**: `docs/VIDEO_SCRAPER_TUTORIAL.md`
- **数据库建表脚本**: `docs/create_exercise_library.sql`
- **AI协作规范**: `CLAUDE.md` 和 `AI_COLLABORATION_RULES.md`

## 🎉 功能特色

1. **专业设计**：采用温暖大地色调设计，避免通用AI设计模式
2. **视频支持**：支持多种视频源（抖音、TikTok、B站、YouTube、本地上传）
3. **智能筛选**：按类别、难度、器械多维度筛选
4. **收藏功能**：用户可收藏喜欢的动作
5. **响应式布局**：完美适配桌面和移动端
6. **视频预览**：缩略图和时长展示
7. **详细信息**：动作要领、注意事项、目标肌群
8. **批量导入**：爬虫工具支持批量下载视频

## 💡 使用建议

1. **视频命名规范**：使用英文或拼音命名，如 `bench-press`、`squat`
2. **视频格式**：推荐使用MP4格式，兼容性最好
3. **视频时长**：建议30-90秒，重点展示动作要领
4. **缩略图尺寸**：推荐 1280x720 或 1920x1080
5. **定期更新**：持续添加新的动作视频，丰富动作库

---

**开发完成！现在你可以在训练计划中链接到动作库，用户可以观看专业的动作指导视频了！💪**
