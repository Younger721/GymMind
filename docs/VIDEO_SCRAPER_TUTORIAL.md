# 抖音/TikTok视频爬虫使用教程

## 📖 功能说明

这个脚本可以帮助你从抖音和TikTok平台批量下载健身动作视频，并自动生成缩略图和元数据，方便导入到GymMind动作库。

## 🔧 环境准备

### 1. 安装Python

确保你的电脑已安装Python 3.7或更高版本。

**检查Python版本：**
```bash
python --version
```

如果未安装，请前往 [Python官网](https://www.python.org/downloads/) 下载安装。

### 2. 安装依赖库

打开命令行（Windows用户按 `Win+R` 输入 `cmd`），运行以下命令：

```bash
pip install requests yt-dlp
```

**依赖说明：**
- `requests`: 用于发送HTTP请求
- `yt-dlp`: 强大的视频下载工具，支持抖音和TikTok

### 3. 安装FFmpeg（可选但推荐）

FFmpeg用于生成视频缩略图和获取视频信息。

**Windows安装：**
1. 访问 [FFmpeg官网](https://ffmpeg.org/download.html)
2. 下载Windows版本
3. 解压后将`bin`目录添加到系统环境变量PATH
4. 验证安装：
   ```bash
   ffmpeg -version
   ```

**Mac安装（使用Homebrew）：**
```bash
brew install ffmpeg
```

**Linux安装：**
```bash
sudo apt-get install ffmpeg  # Ubuntu/Debian
sudo yum install ffmpeg      # CentOS/RHEL
```

## 🚀 快速开始

### 方法1：交互式使用

1. **运行脚本**
   ```bash
   cd D:\java\project\full-project\GymMind
   python scripts/video_scraper.py
   ```

2. **选择操作**
   - 输入 `1`：下载单个视频
   - 输入 `2`：批量下载视频
   - 输入 `3`：查看下载记录
   - 输入 `4`：退出程序

3. **下载单个视频示例**
   ```
   请选择操作：
   1. 下载单个视频
   2. 批量下载视频
   3. 查看下载记录
   4. 退出
   
   请输入选项 (1-4): 1
   
   请输入视频URL: https://www.douyin.com/video/1234567890
   输入文件名（可选，回车跳过）: bench-press
   
   🔍 正在解析抖音视频...
   ✅ 视频下载成功: ./downloaded_videos/bench-press.mp4
   ✅ 缩略图生成成功: ./thumbnails/bench-press.jpg
   ```

4. **批量下载视频示例**
   ```
   请输入选项 (1-4): 2
   
   请输入视频URL列表（每行一个，输入空行结束）：
   https://www.douyin.com/video/1111111111
   https://www.douyin.com/video/2222222222
   https://www.tiktok.com/@user/video/3333333333
   [按回车结束输入]
   
   开始批量下载 3 个视频...
   [1/3] 处理中...
   ✅ 视频下载成功
   [2/3] 处理中...
   ✅ 视频下载成功
   [3/3] 处理中...
   ✅ 视频下载成功
   
   ✅ 下载完成！
      成功: 3
      失败: 0
      总计: 3
   ```

### 方法2：Python代码调用

你也可以在自己的Python脚本中直接调用：

```python
from scripts.video_scraper import VideoScraper

# 创建爬虫实例
scraper = VideoScraper()

# 下载单个视频
video_info = scraper.download_video(
    url='https://www.douyin.com/video/1234567890',
    output_name='squat-tutorial'
)

# 批量下载
urls = [
    'https://www.douyin.com/video/1111111111',
    'https://www.douyin.com/video/2222222222',
    'https://www.tiktok.com/@user/video/3333333333'
]
scraper.batch_download(urls)

# 查看所有下载记录
scraper.show_metadata()
```

## 📁 输出文件说明

脚本运行后会在项目根目录生成以下文件和目录：

```
GymMind/
├── downloaded_videos/        # 下载的视频文件
│   ├── bench-press.mp4
│   ├── squat-tutorial.mp4
│   └── ...
├── thumbnails/              # 视频缩略图
│   ├── bench-press.jpg
│   ├── squat-tutorial.jpg
│   └── ...
└── video_metadata.json      # 视频元数据（JSON格式）
```

### video_metadata.json 格式

```json
[
  {
    "url": "https://www.douyin.com/video/1234567890",
    "platform": "DOUYIN",
    "video_id": "1234567890",
    "file_path": "./downloaded_videos/bench-press.mp4",
    "thumbnail_path": "./thumbnails/bench-press.jpg",
    "duration": 45,
    "download_time": "2026-09-11T10:30:00",
    "output_name": "bench-press"
  }
]
```

## 🎯 如何获取视频URL

### 抖音视频URL获取方法

1. **手机端抖音App**
   - 打开视频
   - 点击右侧「分享」按钮
   - 选择「复制链接」
   - 会得到类似这样的链接：
     ```
     https://v.douyin.com/xxxxx/
     ```
   - 直接使用这个短链接即可

2. **抖音网页版**
   - 打开 https://www.douyin.com
   - 找到目标视频
   - 复制浏览器地址栏中的URL：
     ```
     https://www.douyin.com/video/1234567890123456789
     ```

### TikTok视频URL获取方法

1. **手机端TikTok App**
   - 打开视频
   - 点击右侧「Share」按钮
   - 选择「Copy Link」
   - 会得到类似这样的链接：
     ```
     https://vm.tiktok.com/xxxxx/
     ```

2. **TikTok网页版**
   - 打开 https://www.tiktok.com
   - 找到目标视频
   - 复制浏览器地址栏中的URL：
     ```
     https://www.tiktok.com/@username/video/1234567890123456789
     ```

## 📝 实战案例：批量下载健身动作视频

假设你想为GymMind动作库下载以下健身动作：

### 1. 准备视频清单

创建一个文本文件 `video_list.txt`，每行一个URL：

```
https://www.douyin.com/video/111  # 杠铃卧推教学
https://www.douyin.com/video/222  # 深蹲标准动作
https://www.douyin.com/video/333  # 引体向上技巧
https://www.tiktok.com/@fitness/video/444  # 硬拉教程
https://www.tiktok.com/@gym/video/555  # 肩推动作
```

### 2. 编写批量下载脚本

创建 `batch_download.py`：

```python
from scripts.video_scraper import VideoScraper

# 读取视频列表
with open('video_list.txt', 'r', encoding='utf-8') as f:
    urls = [line.strip().split('#')[0].strip() 
            for line in f if line.strip() and not line.startswith('#')]

# 批量下载
scraper = VideoScraper()
scraper.batch_download(urls)

print("\n下载完成！视频已保存到 downloaded_videos/ 目录")
```

### 3. 运行批量下载

```bash
python batch_download.py
```

## 🔄 将视频导入到GymMind动作库

下载完成后，你需要将视频上传到服务器并导入数据库。

### 方法1：手动通过后台上传

1. 登录GymMind后台管理
2. 进入「动作库管理」
3. 点击「添加动作」
4. 填写动作信息：
   - 上传视频文件（从 `downloaded_videos/` 目录选择）
   - 上传缩略图（从 `thumbnails/` 目录选择）
   - 填写动作名称、类别、难度等信息
5. 点击「保存」

### 方法2：使用API批量导入

创建 `import_to_db.py`：

```python
import json
import requests

# 读取元数据
with open('video_metadata.json', 'r', encoding='utf-8') as f:
    videos = json.load(f)

# GymMind API配置
API_BASE = 'http://localhost:8080/api'
TOKEN = 'your-auth-token'  # 替换为你的认证token

headers = {
    'Authorization': f'Bearer {TOKEN}',
    'Content-Type': 'application/json'
}

# 动作信息映射（需要手动配置）
exercise_info = {
    'bench-press': {
        'name': '杠铃卧推',
        'name_en': 'Barbell Bench Press',
        'category': 'CHEST',
        'difficulty': 'INTERMEDIATE',
        'equipment': 'BARBELL',
        'description': '经典的胸部训练动作',
        'target_muscles': '胸大肌、三角肌前束、肱三头肌',
        'instructions': [
            '躺在平板凳上，双脚平放地面',
            '握距略宽于肩，杠铃下放至胸部',
            '推起杠铃至手臂伸直，保持控制'
        ],
        'tips': [
            '保持肩胛骨后缩下沉',
            '避免弹震式动作',
            '呼气推起，吸气下放'
        ]
    },
    # 添加更多动作信息...
}

# 批量导入
for video in videos:
    name = video['output_name']
    
    if name not in exercise_info:
        print(f"⚠️ 跳过 {name}：未配置动作信息")
        continue
    
    # 准备请求数据
    data = {
        **exercise_info[name],
        'videoUrl': f'/videos/{name}.mp4',  # 需要先上传到服务器
        'videoThumbnail': f'/thumbnails/{name}.jpg',
        'videoDuration': video.get('duration'),
        'videoSource': video['platform']
    }
    
    # 发送请求
    try:
        response = requests.post(
            f'{API_BASE}/exercises',
            headers=headers,
            json=data
        )
        
        if response.status_code == 200:
            print(f"✅ 导入成功: {name}")
        else:
            print(f"❌ 导入失败: {name} - {response.text}")
    
    except Exception as e:
        print(f"❌ 导入出错: {name} - {str(e)}")

print("\n导入完成！")
```

## ⚠️ 注意事项

### 1. 版权问题
- 下载的视频仅供个人学习使用
- 商业使用请联系原作者获得授权
- 尊重内容创作者的版权

### 2. 下载速度
- 脚本会在每个视频下载之间等待3秒，避免请求过快
- 抖音和TikTok可能有访问限制，建议分批下载
- 如遇下载失败，可稍后重试

### 3. 平台限制
- 抖音短链接有时效性，建议尽快下载
- 某些视频可能有地区限制
- 私密视频无法下载

### 4. 技术限制
- 脚本依赖yt-dlp，如果yt-dlp无法解析某些视频，下载会失败
- 部分平台可能更新反爬虫策略，导致脚本失效
- 建议定期更新yt-dlp：`pip install --upgrade yt-dlp`

## 🔧 常见问题

### Q1: 下载失败，提示"未安装yt-dlp"

**解决方法：**
```bash
pip install yt-dlp
```

如果还是失败，尝试：
```bash
python -m pip install yt-dlp
```

### Q2: 无法生成缩略图

**原因：** 未安装FFmpeg

**解决方法：** 参考上面的FFmpeg安装步骤

### Q3: 下载的视频无法播放

**可能原因：**
1. 下载不完整（网络问题）
2. 视频格式不支持

**解决方法：**
1. 删除该视频文件
2. 重新下载
3. 使用VLC等专业播放器测试

### Q4: 抖音视频下载很慢

**解决方法：**
1. 检查网络连接
2. 使用代理（如需要）
3. 分批下载，避免一次性下载太多

### Q5: 如何批量重命名视频文件？

**方法：** 修改 `video_metadata.json` 中的文件名，然后手动重命名文件

或者使用Python脚本：

```python
import json
import os

with open('video_metadata.json', 'r', encoding='utf-8') as f:
    videos = json.load(f)

# 重命名规则
rename_map = {
    'old_name': 'new_name',
    # 添加更多映射...
}

for video in videos:
    old_name = video['output_name']
    if old_name in rename_map:
        new_name = rename_map[old_name]
        
        # 重命名视频文件
        old_video = video['file_path']
        new_video = old_video.replace(old_name, new_name)
        os.rename(old_video, new_video)
        
        # 重命名缩略图
        if video.get('thumbnail_path'):
            old_thumb = video['thumbnail_path']
            new_thumb = old_thumb.replace(old_name, new_name)
            os.rename(old_thumb, new_thumb)
        
        # 更新元数据
        video['output_name'] = new_name
        video['file_path'] = new_video
        video['thumbnail_path'] = new_thumb
        
        print(f"✅ 重命名: {old_name} -> {new_name}")

# 保存更新后的元数据
with open('video_metadata.json', 'w', encoding='utf-8') as f:
    json.dump(videos, f, ensure_ascii=False, indent=2)
```

## 📚 进阶使用

### 自定义输出目录

修改脚本开头的配置：

```python
# 配置
OUTPUT_DIR = "./my_videos"      # 自定义视频目录
THUMBNAIL_DIR = "./my_thumbs"   # 自定义缩略图目录
METADATA_FILE = "./my_data.json" # 自定义元数据文件
```

### 添加自定义元数据

下载后可以手动编辑 `video_metadata.json`，添加更多信息：

```json
{
  "url": "...",
  "platform": "DOUYIN",
  "video_id": "123",
  "file_path": "...",
  
  // 添加自定义字段
  "exercise_name": "杠铃卧推",
  "category": "胸部",
  "difficulty": "中级",
  "notes": "注意肩胛骨后缩"
}
```

## 📞 技术支持

如遇到问题，请检查：

1. Python版本是否 >= 3.7
2. 所有依赖是否正确安装
3. 网络连接是否正常
4. 视频URL是否有效

如果问题仍未解决，请查看错误信息并搜索相关解决方案。

---

**祝你使用愉快！💪**
