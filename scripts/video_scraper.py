#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
抖音/TikTok视频爬虫脚本
用于批量下载健身动作视频到动作库

功能：
1. 支持抖音和TikTok平台
2. 批量下载视频
3. 自动提取视频信息（标题、时长等）
4. 生成缩略图
5. 保存视频元数据
"""

import os
import re
import json
import time
import requests
from urllib.parse import urlparse, parse_qs
from typing import Dict, List, Optional
import subprocess
from pathlib import Path
from datetime import datetime

# 配置
OUTPUT_DIR = "./downloaded_videos"  # 视频输出目录
THUMBNAIL_DIR = "./thumbnails"      # 缩略图目录
METADATA_FILE = "./video_metadata.json"  # 元数据文件

# 请求头（模拟浏览器）
HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
    'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
}


class VideoScraper:
    """视频爬虫类"""

    def __init__(self):
        """初始化爬虫"""
        self.session = requests.Session()
        self.session.headers.update(HEADERS)
        self.metadata = self._load_metadata()

        # 创建输出目录
        os.makedirs(OUTPUT_DIR, exist_ok=True)
        os.makedirs(THUMBNAIL_DIR, exist_ok=True)

    def _load_metadata(self) -> List[Dict]:
        """加载已有的元数据"""
        if os.path.exists(METADATA_FILE):
            with open(METADATA_FILE, 'r', encoding='utf-8') as f:
                return json.load(f)
        return []

    def _save_metadata(self):
        """保存元数据到文件"""
        with open(METADATA_FILE, 'w', encoding='utf-8') as f:
            json.dump(self.metadata, f, ensure_ascii=False, indent=2)

    def download_video(self, url: str, output_name: Optional[str] = None) -> Optional[Dict]:
        """
        下载视频

        Args:
            url: 视频URL
            output_name: 输出文件名（可选）

        Returns:
            视频信息字典，失败返回None
        """
        print(f"\n开始处理视频: {url}")

        # 检测平台
        if 'douyin.com' in url or 'iesdouyin.com' in url:
            return self._download_douyin(url, output_name)
        elif 'tiktok.com' in url:
            return self._download_tiktok(url, output_name)
        else:
            print(f"❌ 不支持的平台: {url}")
            return None

    def _download_douyin(self, url: str, output_name: Optional[str] = None) -> Optional[Dict]:
        """下载抖音视频"""
        try:
            print("🔍 正在解析抖音视频...")

            # 获取真实视频地址
            response = self.session.get(url, allow_redirects=True, timeout=10)

            # 这里需要解析抖音的视频地址
            # 注意：抖音的视频解析可能需要额外的API或第三方工具
            # 以下是简化示例，实际使用需要更复杂的解析逻辑

            # 提取视频ID
            video_id = self._extract_douyin_id(url)
            if not video_id:
                print("❌ 无法提取视频ID")
                return None

            # 使用yt-dlp下载（如果安装了）
            if output_name is None:
                output_name = f"douyin_{video_id}_{int(time.time())}"

            output_path = os.path.join(OUTPUT_DIR, f"{output_name}.mp4")

            # 尝试使用yt-dlp下载
            if self._download_with_ytdlp(url, output_path):
                print(f"✅ 视频下载成功: {output_path}")

                # 生成缩略图
                thumbnail_path = self._generate_thumbnail(output_path)

                # 获取视频时长
                duration = self._get_video_duration(output_path)

                # 保存元数据
                video_info = {
                    'url': url,
                    'platform': 'DOUYIN',
                    'video_id': video_id,
                    'file_path': output_path,
                    'thumbnail_path': thumbnail_path,
                    'duration': duration,
                    'download_time': datetime.now().isoformat(),
                    'output_name': output_name
                }

                self.metadata.append(video_info)
                self._save_metadata()

                return video_info
            else:
                print("❌ 下载失败")
                return None

        except Exception as e:
            print(f"❌ 下载抖音视频出错: {str(e)}")
            return None

    def _download_tiktok(self, url: str, output_name: Optional[str] = None) -> Optional[Dict]:
        """下载TikTok视频"""
        try:
            print("🔍 正在解析TikTok视频...")

            # 提取视频ID
            video_id = self._extract_tiktok_id(url)
            if not video_id:
                print("❌ 无法提取视频ID")
                return None

            if output_name is None:
                output_name = f"tiktok_{video_id}_{int(time.time())}"

            output_path = os.path.join(OUTPUT_DIR, f"{output_name}.mp4")

            # 使用yt-dlp下载
            if self._download_with_ytdlp(url, output_path):
                print(f"✅ 视频下载成功: {output_path}")

                # 生成缩略图
                thumbnail_path = self._generate_thumbnail(output_path)

                # 获取视频时长
                duration = self._get_video_duration(output_path)

                # 保存元数据
                video_info = {
                    'url': url,
                    'platform': 'TIKTOK',
                    'video_id': video_id,
                    'file_path': output_path,
                    'thumbnail_path': thumbnail_path,
                    'duration': duration,
                    'download_time': datetime.now().isoformat(),
                    'output_name': output_name
                }

                self.metadata.append(video_info)
                self._save_metadata()

                return video_info
            else:
                print("❌ 下载失败")
                return None

        except Exception as e:
            print(f"❌ 下载TikTok视频出错: {str(e)}")
            return None

    def _download_with_ytdlp(self, url: str, output_path: str) -> bool:
        """使用yt-dlp下载视频"""
        try:
            # 检查yt-dlp是否安装
            result = subprocess.run(['yt-dlp', '--version'],
                                  capture_output=True,
                                  text=True)

            if result.returncode != 0:
                print("❌ 未安装yt-dlp，请先安装")
                return False

            # 下载视频
            cmd = [
                'yt-dlp',
                '-o', output_path,
                '--no-warnings',
                url
            ]

            result = subprocess.run(cmd, capture_output=True, text=True)
            return result.returncode == 0

        except FileNotFoundError:
            print("❌ 未找到yt-dlp，请先安装: pip install yt-dlp")
            return False
        except Exception as e:
            print(f"❌ yt-dlp下载出错: {str(e)}")
            return False

    def _generate_thumbnail(self, video_path: str) -> Optional[str]:
        """生成视频缩略图"""
        try:
            # 检查ffmpeg是否安装
            result = subprocess.run(['ffmpeg', '-version'],
                                  capture_output=True,
                                  text=True)

            if result.returncode != 0:
                print("⚠️ 未安装ffmpeg，跳过缩略图生成")
                return None

            # 生成缩略图
            video_name = os.path.basename(video_path)
            thumbnail_name = os.path.splitext(video_name)[0] + '.jpg'
            thumbnail_path = os.path.join(THUMBNAIL_DIR, thumbnail_name)

            cmd = [
                'ffmpeg',
                '-i', video_path,
                '-ss', '00:00:01',  # 截取第1秒
                '-vframes', '1',
                '-q:v', '2',
                thumbnail_path,
                '-y'
            ]

            result = subprocess.run(cmd, capture_output=True, text=True)

            if result.returncode == 0:
                print(f"✅ 缩略图生成成功: {thumbnail_path}")
                return thumbnail_path
            else:
                print("⚠️ 缩略图生成失败")
                return None

        except FileNotFoundError:
            print("⚠️ 未找到ffmpeg，跳过缩略图生成")
            return None
        except Exception as e:
            print(f"⚠️ 生成缩略图出错: {str(e)}")
            return None

    def _get_video_duration(self, video_path: str) -> Optional[int]:
        """获取视频时长（秒）"""
        try:
            cmd = [
                'ffprobe',
                '-v', 'error',
                '-show_entries', 'format=duration',
                '-of', 'default=noprint_wrappers=1:nokey=1',
                video_path
            ]

            result = subprocess.run(cmd, capture_output=True, text=True)

            if result.returncode == 0:
                duration = float(result.stdout.strip())
                return int(duration)
            else:
                return None

        except Exception as e:
            print(f"⚠️ 获取视频时长出错: {str(e)}")
            return None

    def _extract_douyin_id(self, url: str) -> Optional[str]:
        """提取抖音视频ID"""
        # 尝试多种模式匹配
        patterns = [
            r'video/(\d+)',
            r'share/video/(\d+)',
            r'/(\d{19})',
        ]

        for pattern in patterns:
            match = re.search(pattern, url)
            if match:
                return match.group(1)

        return None

    def _extract_tiktok_id(self, url: str) -> Optional[str]:
        """提取TikTok视频ID"""
        # TikTok视频ID模式
        patterns = [
            r'video/(\d+)',
            r'/v/(\d+)',
        ]

        for pattern in patterns:
            match = re.search(pattern, url)
            if match:
                return match.group(1)

        return None

    def batch_download(self, urls: List[str]):
        """批量下载视频"""
        print(f"\n开始批量下载 {len(urls)} 个视频...\n")

        success_count = 0
        fail_count = 0

        for i, url in enumerate(urls, 1):
            print(f"\n[{i}/{len(urls)}] 处理中...")

            result = self.download_video(url)

            if result:
                success_count += 1
            else:
                fail_count += 1

            # 避免请求过快
            if i < len(urls):
                print("⏳ 等待3秒...")
                time.sleep(3)

        print(f"\n{'='*50}")
        print(f"✅ 下载完成！")
        print(f"   成功: {success_count}")
        print(f"   失败: {fail_count}")
        print(f"   总计: {len(urls)}")
        print(f"{'='*50}\n")

    def show_metadata(self):
        """显示所有下载的视频元数据"""
        if not self.metadata:
            print("暂无下载记录")
            return

        print(f"\n共有 {len(self.metadata)} 条记录:\n")
        for i, item in enumerate(self.metadata, 1):
            print(f"{i}. {item['output_name']}")
            print(f"   平台: {item['platform']}")
            print(f"   时长: {item.get('duration', '未知')}秒")
            print(f"   文件: {item['file_path']}")
            print(f"   缩略图: {item.get('thumbnail_path', '无')}")
            print()


def main():
    """主函数"""
    print("""
╔══════════════════════════════════════════════════════════╗
║        抖音/TikTok视频爬虫 - GymMind动作库工具          ║
╚══════════════════════════════════════════════════════════╝
    """)

    scraper = VideoScraper()

    while True:
        print("\n请选择操作：")
        print("1. 下载单个视频")
        print("2. 批量下载视频")
        print("3. 查看下载记录")
        print("4. 退出")

        choice = input("\n请输入选项 (1-4): ").strip()

        if choice == '1':
            url = input("\n请输入视频URL: ").strip()
            if url:
                name = input("输入文件名（可选，回车跳过）: ").strip() or None
                scraper.download_video(url, name)
            else:
                print("❌ URL不能为空")

        elif choice == '2':
            print("\n请输入视频URL列表（每行一个，输入空行结束）：")
            urls = []
            while True:
                url = input().strip()
                if not url:
                    break
                urls.append(url)

            if urls:
                scraper.batch_download(urls)
            else:
                print("❌ 未输入任何URL")

        elif choice == '3':
            scraper.show_metadata()

        elif choice == '4':
            print("\n👋 再见！")
            break

        else:
            print("❌ 无效选项，请重新输入")


if __name__ == '__main__':
    main()
