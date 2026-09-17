<template>
  <div class="social-container">
    <!-- Header with tabs -->
    <div class="social-header">
      <h2>健身动态</h2>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="最新" name="feed"></el-tab-pane>
        <el-tab-pane label="热门" name="trending"></el-tab-pane>
        <el-tab-pane label="我的" name="my"></el-tab-pane>
      </el-tabs>
    </div>

    <!-- Create Post Card -->
    <el-card class="create-post-card" shadow="hover">
      <el-input
        v-model="newPost.content"
        type="textarea"
        :rows="3"
        placeholder="分享你的健身动态..."
        maxlength="500"
        show-word-limit
      />
      <div class="post-actions">
        <el-select v-model="newPost.postType" placeholder="选择类型" style="width: 120px">
          <el-option label="训练打卡" value="WORKOUT"></el-option>
          <el-option label="进度照片" value="PROGRESS"></el-option>
          <el-option label="饮食记录" value="MEAL"></el-option>
          <el-option label="日常分享" value="GENERAL"></el-option>
        </el-select>
        <el-button type="primary" @click="handleCreatePost" :loading="posting">
          发布动态
        </el-button>
      </div>
    </el-card>

    <!-- Posts List -->
    <div class="posts-list" v-loading="loading">
      <el-card
        v-for="post in posts"
        :key="post.id"
        class="post-card"
        shadow="hover"
      >
        <!-- Post Header -->
        <div class="post-header">
          <div class="user-info">
            <el-avatar :size="40" :src="post.userAvatar || undefined">
              {{ post.userName?.charAt(0) }}
            </el-avatar>
            <div class="user-details">
              <span class="username">{{ post.userName }}</span>
              <span class="post-time">{{ formatTime(post.createdAt) }}</span>
            </div>
          </div>
          <el-tag :type="getPostTypeTag(post.postType)" size="small">
            {{ getPostTypeLabel(post.postType) }}
          </el-tag>
        </div>

        <!-- Post Content -->
        <div class="post-content">
          <p>{{ post.content }}</p>
          <div v-if="post.imageUrls && post.imageUrls.length" class="post-images">
            <el-image
              v-for="(url, index) in post.imageUrls"
              :key="index"
              :src="url"
              fit="cover"
              :preview-src-list="post.imageUrls"
            />
          </div>
        </div>

        <!-- Post Actions -->
        <div class="post-actions-bar">
          <div class="action-item" @click="handleLike(post)">
            <el-icon :class="{ liked: post.isLiked }">
              <component :is="post.isLiked ? 'HeartFilled' : 'Heart'" />
            </el-icon>
            <span>{{ post.likes }}</span>
          </div>
          <div class="action-item" @click="handleShowComments(post)">
            <el-icon><ChatDotRound /></el-icon>
            <span>{{ post.comments }}</span>
          </div>
        </div>

        <!-- Comments Section -->
        <div v-if="showCommentsForPost === post.id" class="comments-section">
          <div class="add-comment">
            <el-input
              v-model="commentText"
              placeholder="写下你的评论..."
              @keyup.enter="handleAddComment(post.id)"
            >
              <template #append>
                <el-button @click="handleAddComment(post.id)">发送</el-button>
              </template>
            </el-input>
          </div>
          <div v-loading="loadingComments" class="comments-list">
            <div
              v-for="comment in postComments"
              :key="comment.id"
              class="comment-item"
            >
              <el-avatar :size="30" :src="comment.userAvatar || undefined">
                {{ comment.userName?.charAt(0) }}
              </el-avatar>
              <div class="comment-content">
                <div class="comment-header">
                  <span class="comment-user">{{ comment.userName }}</span>
                  <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
                </div>
                <p>{{ comment.content }}</p>
              </div>
            </div>
            <el-empty v-if="!loadingComments && postComments.length === 0" description="暂无评论" />
          </div>
        </div>
      </el-card>

      <!-- Load More -->
      <div v-if="hasMore" class="load-more">
        <el-button @click="loadMore" :loading="loading">加载更多</el-button>
      </div>

      <el-empty v-if="!loading && posts.length === 0" description="暂无动态" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Heart,
  HeartFilled,
  ChatDotRound
} from '@element-plus/icons-vue'
import {
  getPostFeed,
  getTrendingPosts,
  getUserPosts,
  createPost,
  likePost,
  unlikePost,
  getComments,
  addComment,
  type PostResponse,
  type CommentResponse
} from '@/api/social'

const activeTab = ref('feed')
const posts = ref<PostResponse[]>([])
const loading = ref(false)
const posting = ref(false)
const currentPage = ref(0)
const hasMore = ref(true)

// Create post
const newPost = ref({
  content: '',
  postType: 'WORKOUT'
})

// Comments
const showCommentsForPost = ref<number | null>(null)
const postComments = ref<CommentResponse[]>([])
const commentText = ref('')
const loadingComments = ref(false)

onMounted(() => {
  loadPosts()
})

const handleTabChange = () => {
  currentPage.value = 0
  posts.value = []
  hasMore.value = true
  loadPosts()
}

const loadPosts = async () => {
  loading.value = true
  try {
    let response
    if (activeTab.value === 'feed') {
      response = await getPostFeed(currentPage.value, 10)
    } else if (activeTab.value === 'trending') {
      response = await getTrendingPosts(24, currentPage.value, 10)
    } else {
      // Get current user's posts - you'd need to get userId from store/auth
      const userId = 1 // Replace with actual user ID
      response = await getUserPosts(userId, currentPage.value, 10)
    }

    if (currentPage.value === 0) {
      posts.value = response.content
    } else {
      posts.value.push(...response.content)
    }

    hasMore.value = currentPage.value < response.totalPages - 1
  } catch (error) {
    ElMessage.error('加载动态失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  currentPage.value++
  loadPosts()
}

const handleCreatePost = async () => {
  if (!newPost.value.content.trim()) {
    ElMessage.warning('请输入动态内容')
    return
  }

  posting.value = true
  try {
    await createPost({
      content: newPost.value.content,
      postType: newPost.value.postType as any
    })

    ElMessage.success('发布成功')
    newPost.value.content = ''
    newPost.value.postType = 'WORKOUT'

    // Reload posts
    currentPage.value = 0
    loadPosts()
  } catch (error) {
    ElMessage.error('发布失败')
    console.error(error)
  } finally {
    posting.value = false
  }
}

const handleLike = async (post: PostResponse) => {
  try {
    if (post.isLiked) {
      await unlikePost(post.id)
      post.likes--
      post.isLiked = false
    } else {
      await likePost(post.id)
      post.likes++
      post.isLiked = true
    }
  } catch (error) {
    ElMessage.error('操作失败')
    console.error(error)
  }
}

const handleShowComments = async (post: PostResponse) => {
  if (showCommentsForPost.value === post.id) {
    showCommentsForPost.value = null
    return
  }

  showCommentsForPost.value = post.id
  loadingComments.value = true

  try {
    postComments.value = await getComments(post.id)
  } catch (error) {
    ElMessage.error('加载评论失败')
    console.error(error)
  } finally {
    loadingComments.value = false
  }
}

const handleAddComment = async (postId: number) => {
  if (!commentText.value.trim()) {
    return
  }

  try {
    const comment = await addComment(postId, { content: commentText.value })
    postComments.value.unshift(comment)
    commentText.value = ''

    // Update comment count
    const post = posts.value.find(p => p.id === postId)
    if (post) {
      post.comments++
    }

    ElMessage.success('评论成功')
  } catch (error) {
    ElMessage.error('评论失败')
    console.error(error)
  }
}

const getPostTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    WORKOUT: '训练打卡',
    PROGRESS: '进度照片',
    MEAL: '饮食记录',
    GENERAL: '日常分享'
  }
  return labels[type] || type
}

const getPostTypeTag = (type: string) => {
  const tags: Record<string, any> = {
    WORKOUT: 'success',
    PROGRESS: 'warning',
    MEAL: 'danger',
    GENERAL: 'info'
  }
  return tags[type] || 'info'
}

const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`

  return date.toLocaleDateString('zh-CN')
}
</script>

<style scoped lang="scss">
.social-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.social-header {
  margin-bottom: 20px;

  h2 {
    margin-bottom: 10px;
  }
}

.create-post-card {
  margin-bottom: 20px;

  .post-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 10px;
  }
}

.posts-list {
  .post-card {
    margin-bottom: 20px;

    .post-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 15px;

      .user-info {
        display: flex;
        align-items: center;
        gap: 10px;

        .user-details {
          display: flex;
          flex-direction: column;

          .username {
            font-weight: 600;
            font-size: 14px;
          }

          .post-time {
            font-size: 12px;
            color: #909399;
          }
        }
      }
    }

    .post-content {
      margin-bottom: 15px;

      p {
        margin-bottom: 10px;
        line-height: 1.6;
      }

      .post-images {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
        gap: 10px;

        .el-image {
          width: 100%;
          height: 150px;
          border-radius: 8px;
          cursor: pointer;
        }
      }
    }

    .post-actions-bar {
      display: flex;
      gap: 30px;
      padding-top: 15px;
      border-top: 1px solid #ebeef5;

      .action-item {
        display: flex;
        align-items: center;
        gap: 5px;
        cursor: pointer;
        transition: all 0.3s;

        &:hover {
          color: #409eff;
        }

        .el-icon {
          font-size: 20px;

          &.liked {
            color: #f56c6c;
          }
        }
      }
    }

    .comments-section {
      margin-top: 15px;
      padding-top: 15px;
      border-top: 1px solid #ebeef5;

      .add-comment {
        margin-bottom: 15px;
      }

      .comments-list {
        .comment-item {
          display: flex;
          gap: 10px;
          margin-bottom: 15px;

          .comment-content {
            flex: 1;

            .comment-header {
              display: flex;
              justify-content: space-between;
              margin-bottom: 5px;

              .comment-user {
                font-weight: 600;
                font-size: 13px;
              }

              .comment-time {
                font-size: 12px;
                color: #909399;
              }
            }

            p {
              font-size: 14px;
              line-height: 1.5;
            }
          }
        }
      }
    }
  }

  .load-more {
    text-align: center;
    margin-top: 20px;
  }
}
</style>
