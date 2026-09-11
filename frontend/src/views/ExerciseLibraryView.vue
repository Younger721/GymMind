<template>
  <div class="exercise-library">
    <!-- 页面头部 -->
    <header class="library-header">
      <div class="header-content">
        <div class="title-section">
          <h1>动作库</h1>
          <p class="subtitle">专业健身动作视频指导</p>
        </div>

        <!-- 搜索框 -->
        <div class="search-box">
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索动作名称..."
            @keyup.enter="handleSearch"
          />
          <button @click="handleSearch" class="search-btn">
            <span>搜索</span>
          </button>
        </div>
      </div>
    </header>

    <!-- 筛选器 -->
    <div class="filters">
      <div class="filter-group">
        <label>类别</label>
        <select v-model="filters.category" @change="applyFilters">
          <option value="">全部</option>
          <option value="CHEST">胸部</option>
          <option value="BACK">背部</option>
          <option value="LEGS">腿部</option>
          <option value="SHOULDERS">肩部</option>
          <option value="ARMS">手臂</option>
          <option value="CORE">核心</option>
          <option value="CARDIO">有氧</option>
        </select>
      </div>

      <div class="filter-group">
        <label>难度</label>
        <select v-model="filters.difficulty" @change="applyFilters">
          <option value="">全部</option>
          <option value="BEGINNER">初级</option>
          <option value="INTERMEDIATE">中级</option>
          <option value="ADVANCED">高级</option>
        </select>
      </div>

      <div class="filter-group">
        <label>器械</label>
        <select v-model="filters.equipment" @change="applyFilters">
          <option value="">全部</option>
          <option value="BARBELL">杠铃</option>
          <option value="DUMBBELL">哑铃</option>
          <option value="MACHINE">器械</option>
          <option value="BODYWEIGHT">自重</option>
          <option value="CABLE">绳索</option>
        </select>
      </div>

      <div class="filter-group">
        <label>排序</label>
        <select v-model="filters.sortBy" @change="applyFilters">
          <option value="default">默认</option>
          <option value="popular">最热门</option>
          <option value="liked">最多赞</option>
          <option value="newest">最新</option>
        </select>
      </div>

      <button @click="resetFilters" class="reset-btn">重置筛选</button>
    </div>

    <!-- 快捷标签 -->
    <div class="quick-tabs">
      <button @click="loadPopular" class="tab-btn">🔥 热门动作</button>
      <button @click="loadRecommended" class="tab-btn">⭐ 推荐动作</button>
      <button @click="loadFavorites" class="tab-btn">❤️ 我的收藏</button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 动作网格 -->
    <div v-else class="exercise-grid">
      <div
        v-for="exercise in exercises"
        :key="exercise.id"
        class="exercise-card"
        @click="openExerciseDetail(exercise.id)"
      >
        <!-- 视频缩略图 -->
        <div class="video-thumbnail">
          <img
            :src="exercise.videoThumbnail || '/placeholder-video.jpg'"
            :alt="exercise.name"
          />
          <div class="video-duration" v-if="exercise.videoDuration">
            {{ formatDuration(exercise.videoDuration) }}
          </div>
          <div class="verified-badge" v-if="exercise.isVerified">
            <span>✓</span>
          </div>
        </div>

        <!-- 动作信息 -->
        <div class="exercise-info">
          <h3 class="exercise-name">{{ exercise.name }}</h3>
          <p class="exercise-name-en">{{ exercise.nameEn }}</p>

          <div class="exercise-tags">
            <span class="tag category">{{ getCategoryText(exercise.category) }}</span>
            <span class="tag difficulty" :class="exercise.difficulty.toLowerCase()">
              {{ getDifficultyText(exercise.difficulty) }}
            </span>
            <span class="tag equipment" v-if="exercise.equipment">
              {{ getEquipmentText(exercise.equipment) }}
            </span>
          </div>

          <p class="target-muscles" v-if="exercise.targetMuscles">
            <strong>目标肌群：</strong>{{ exercise.targetMuscles }}
          </p>

          <div class="exercise-stats">
            <span>👁 {{ exercise.viewCount }}</span>
            <span>👍 {{ exercise.likeCount }}</span>
            <button
              @click.stop="toggleFavorite(exercise.id)"
              class="favorite-btn"
              :class="{ favorited: exercise.isFavorited }"
            >
              {{ exercise.isFavorited ? '❤️' : '🤍' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && exercises.length === 0" class="empty-state">
      <p>暂无动作数据</p>
      <button @click="resetFilters" class="btn-primary">重置筛选</button>
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="pagination">
      <button
        @click="changePage(currentPage - 1)"
        :disabled="currentPage === 0"
        class="page-btn"
      >
        上一页
      </button>

      <span class="page-info">
        第 {{ currentPage + 1 }} 页 / 共 {{ totalPages }} 页
      </span>

      <button
        @click="changePage(currentPage + 1)"
        :disabled="currentPage >= totalPages - 1"
        class="page-btn"
      >
        下一页
      </button>
    </div>

    <!-- 动作详情弹窗 -->
    <div v-if="showDetailModal" class="modal-overlay" @click="closeDetailModal">
      <div class="modal-content" @click.stop>
        <button class="close-btn" @click="closeDetailModal">×</button>

        <div v-if="selectedExercise" class="exercise-detail">
          <h2>{{ selectedExercise.name }}</h2>
          <p class="name-en">{{ selectedExercise.nameEn }}</p>

          <!-- 视频播放器 -->
          <div class="video-player">
            <video
              :src="selectedExercise.videoUrl"
              controls
              :poster="selectedExercise.videoThumbnail"
            >
              您的浏览器不支持视频播放
            </video>
          </div>

          <div class="detail-tags">
            <span class="tag">{{ getCategoryText(selectedExercise.category) }}</span>
            <span class="tag">{{ getDifficultyText(selectedExercise.difficulty) }}</span>
            <span class="tag" v-if="selectedExercise.equipment">
              {{ getEquipmentText(selectedExercise.equipment) }}
            </span>
          </div>

          <div class="detail-section" v-if="selectedExercise.description">
            <h3>动作描述</h3>
            <p>{{ selectedExercise.description }}</p>
          </div>

          <div class="detail-section" v-if="selectedExercise.targetMuscles">
            <h3>目标肌群</h3>
            <p>{{ selectedExercise.targetMuscles }}</p>
          </div>

          <div class="detail-section" v-if="selectedExercise.instructions && selectedExercise.instructions.length">
            <h3>动作要领</h3>
            <ol>
              <li v-for="(step, index) in selectedExercise.instructions" :key="index">
                {{ step }}
              </li>
            </ol>
          </div>

          <div class="detail-section" v-if="selectedExercise.tips && selectedExercise.tips.length">
            <h3>注意事项</h3>
            <ul>
              <li v-for="(tip, index) in selectedExercise.tips" :key="index">
                {{ tip }}
              </li>
            </ul>
          </div>

          <div class="detail-actions">
            <button
              @click="toggleFavorite(selectedExercise.id)"
              class="btn-favorite"
              :class="{ favorited: selectedExercise.isFavorited }"
            >
              {{ selectedExercise.isFavorited ? '❤️ 已收藏' : '🤍 收藏' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import apiClient from '@/api/client'

// 路由
const router = useRouter()

// 响应式数据
const loading = ref(false)
const exercises = ref<any[]>([])
const searchKeyword = ref('')
const currentPage = ref(0)
const totalPages = ref(0)
const pageSize = ref(12)

// 筛选器
const filters = ref({
  category: '',
  difficulty: '',
  equipment: '',
  sortBy: 'default'
})

// 动作详情弹窗
const showDetailModal = ref(false)
const selectedExercise = ref<any>(null)

/**
 * 加载动作列表
 */
const loadExercises = async () => {
  try {
    loading.value = true

    const params: any = {
      page: currentPage.value,
      size: pageSize.value,
      sortBy: filters.value.sortBy
    }

    if (filters.value.category) params.category = filters.value.category
    if (filters.value.difficulty) params.difficulty = filters.value.difficulty
    if (filters.value.equipment) params.equipment = filters.value.equipment
    if (searchKeyword.value) params.keyword = searchKeyword.value

    const response = await apiClient.get('/exercises', { params })

    exercises.value = response.data.data.content
    totalPages.value = response.data.data.totalPages
    currentPage.value = response.data.data.number
  } catch (error: any) {
    console.error('加载动作列表失败:', error)
    alert('加载失败: ' + (error.response?.data?.message || '网络错误'))
  } finally {
    loading.value = false
  }
}

/**
 * 应用筛选
 */
const applyFilters = () => {
  currentPage.value = 0
  loadExercises()
}

/**
 * 重置筛选
 */
const resetFilters = () => {
  filters.value = {
    category: '',
    difficulty: '',
    equipment: '',
    sortBy: 'default'
  }
  searchKeyword.value = ''
  currentPage.value = 0
  loadExercises()
}

/**
 * 搜索
 */
const handleSearch = () => {
  currentPage.value = 0
  loadExercises()
}

/**
 * 切换页码
 */
const changePage = (page: number) => {
  currentPage.value = page
  loadExercises()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

/**
 * 加载热门动作
 */
const loadPopular = async () => {
  try {
    loading.value = true
    const response = await apiClient.get('/exercises/popular')
    exercises.value = response.data.data
    totalPages.value = 1
    currentPage.value = 0
  } catch (error) {
    console.error('加载热门动作失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 加载推荐动作
 */
const loadRecommended = async () => {
  try {
    loading.value = true
    const response = await apiClient.get('/exercises/recommended')
    exercises.value = response.data.data
    totalPages.value = 1
    currentPage.value = 0
  } catch (error) {
    console.error('加载推荐动作失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 加载我的收藏
 */
const loadFavorites = async () => {
  try {
    loading.value = true
    const response = await apiClient.get('/exercises/favorites')
    exercises.value = response.data.data
    totalPages.value = 1
    currentPage.value = 0
  } catch (error: any) {
    console.error('加载收藏失败:', error)
    if (error.response?.status === 401) {
      alert('请先登录')
      router.push('/login')
    }
  } finally {
    loading.value = false
  }
}

/**
 * 打开动作详情
 */
const openExerciseDetail = async (id: number) => {
  try {
    const response = await apiClient.get(`/exercises/${id}`)
    selectedExercise.value = response.data.data
    showDetailModal.value = true
  } catch (error) {
    console.error('加载动作详情失败:', error)
    alert('加载详情失败')
  }
}

/**
 * 关闭详情弹窗
 */
const closeDetailModal = () => {
  showDetailModal.value = false
  selectedExercise.value = null
}

/**
 * 收藏/取消收藏
 */
const toggleFavorite = async (id: number) => {
  try {
    await apiClient.post(`/exercises/${id}/favorite`)

    // 更新列表中的收藏状态
    const exercise = exercises.value.find(e => e.id === id)
    if (exercise) {
      exercise.isFavorited = !exercise.isFavorited
    }

    // 更新详情弹窗中的收藏状态
    if (selectedExercise.value && selectedExercise.value.id === id) {
      selectedExercise.value.isFavorited = !selectedExercise.value.isFavorited
    }
  } catch (error: any) {
    console.error('收藏操作失败:', error)
    if (error.response?.status === 401) {
      alert('请先登录')
      router.push('/login')
    }
  }
}

/**
 * 格式化视频时长
 */
const formatDuration = (seconds: number): string => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

/**
 * 获取类别文本
 */
const getCategoryText = (category: string): string => {
  const map: Record<string, string> = {
    CHEST: '胸部',
    BACK: '背部',
    LEGS: '腿部',
    SHOULDERS: '肩部',
    ARMS: '手臂',
    CORE: '核心',
    CARDIO: '有氧'
  }
  return map[category] || category
}

/**
 * 获取难度文本
 */
const getDifficultyText = (difficulty: string): string => {
  const map: Record<string, string> = {
    BEGINNER: '初级',
    INTERMEDIATE: '中级',
    ADVANCED: '高级'
  }
  return map[difficulty] || difficulty
}

/**
 * 获取器械文本
 */
const getEquipmentText = (equipment: string): string => {
  const map: Record<string, string> = {
    BARBELL: '杠铃',
    DUMBBELL: '哑铃',
    MACHINE: '器械',
    BODYWEIGHT: '自重',
    CABLE: '绳索'
  }
  return map[equipment] || equipment
}

// 页面加载时获取数据
onMounted(() => {
  loadExercises()
})
</script>

<style scoped>
.exercise-library {
  max-width: 1400px;
  margin: 0 auto;
  padding: 2rem;
}

/* 页面头部 */
.library-header {
  margin-bottom: 2rem;
  padding: 2rem;
  background: linear-gradient(135deg, var(--terracotta, #C4612F) 0%, #8B4513 100%);
  border-radius: 16px;
  color: white;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 2rem;
}

.title-section h1 {
  font-family: var(--font-serif, 'Fraunces', serif);
  font-size: 2.5rem;
  margin: 0 0 0.5rem 0;
}

.subtitle {
  margin: 0;
  opacity: 0.9;
  font-size: 1.1rem;
}

.search-box {
  display: flex;
  gap: 0.5rem;
  flex: 1;
  max-width: 500px;
}

.search-box input {
  flex: 1;
  padding: 0.75rem 1rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
}

.search-btn {
  padding: 0.75rem 1.5rem;
  background: white;
  color: var(--terracotta, #C4612F);
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s;
}

.search-btn:hover {
  transform: translateY(-2px);
}

/* 筛选器 */
.filters {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.5rem;
  padding: 1.5rem;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.filter-group label {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--ink, #1F2421);
}

.filter-group select {
  padding: 0.5rem 1rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 0.95rem;
  background: white;
  cursor: pointer;
}

.reset-btn {
  padding: 0.5rem 1.5rem;
  background: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  align-self: flex-end;
}

.reset-btn:hover {
  background: #e5e5e5;
}

/* 快捷标签 */
.quick-tabs {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
}

.tab-btn {
  padding: 0.75rem 1.5rem;
  background: white;
  border: 2px solid var(--terracotta, #C4612F);
  border-radius: 8px;
  color: var(--terracotta, #C4612F);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn:hover {
  background: var(--terracotta, #C4612F);
  color: white;
}

/* 加载状态 */
.loading {
  text-align: center;
  padding: 4rem 0;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid var(--terracotta, #C4612F);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 动作网格 */
.exercise-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.exercise-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.exercise-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
}

.video-thumbnail {
  position: relative;
  width: 100%;
  padding-top: 56.25%; /* 16:9 比例 */
  background: #f0f0f0;
  overflow: hidden;
}

.video-thumbnail img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-duration {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0,0,0,0.8);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 0.85rem;
}

.verified-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  background: #4CAF50;
  color: white;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

.exercise-info {
  padding: 1rem;
}

.exercise-name {
  font-size: 1.2rem;
  font-weight: 700;
  margin: 0 0 0.25rem 0;
  color: var(--ink, #1F2421);
}

.exercise-name-en {
  font-size: 0.9rem;
  color: #666;
  margin: 0 0 0.75rem 0;
}

.exercise-tags {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-bottom: 0.75rem;
}

.tag {
  padding: 0.25rem 0.75rem;
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: 600;
}

.tag.category {
  background: #e3f2fd;
  color: #1976d2;
}

.tag.difficulty {
  background: #fff3e0;
  color: #f57c00;
}

.tag.difficulty.beginner {
  background: #e8f5e9;
  color: #388e3c;
}

.tag.difficulty.advanced {
  background: #ffebee;
  color: #d32f2f;
}

.tag.equipment {
  background: #f3e5f5;
  color: #7b1fa2;
}

.target-muscles {
  font-size: 0.9rem;
  color: #555;
  margin: 0.5rem 0;
}

.exercise-stats {
  display: flex;
  gap: 1rem;
  align-items: center;
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px solid #eee;
}

.exercise-stats span {
  font-size: 0.9rem;
  color: #666;
}

.favorite-btn {
  margin-left: auto;
  background: none;
  border: none;
  font-size: 1.2rem;
  cursor: pointer;
  transition: transform 0.2s;
}

.favorite-btn:hover {
  transform: scale(1.2);
}

.favorite-btn.favorited {
  animation: heartbeat 0.3s;
}

@keyframes heartbeat {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.3); }
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 4rem 0;
  color: #666;
}

.btn-primary {
  margin-top: 1rem;
  padding: 0.75rem 2rem;
  background: var(--terracotta, #C4612F);
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  margin: 2rem 0;
}

.page-btn {
  padding: 0.5rem 1rem;
  background: white;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-btn:not(:disabled):hover {
  background: #f5f5f5;
}

.page-info {
  font-weight: 600;
  color: var(--ink, #1F2421);
}

/* 弹窗 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 2rem;
  overflow-y: auto;
}

.modal-content {
  background: white;
  border-radius: 16px;
  max-width: 900px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  position: relative;
  padding: 2rem;
}

.close-btn {
  position: absolute;
  top: 1rem;
  right: 1rem;
  background: #f5f5f5;
  border: none;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  font-size: 1.5rem;
  cursor: pointer;
  z-index: 1;
}

.close-btn:hover {
  background: #e5e5e5;
}

.exercise-detail h2 {
  font-family: var(--font-serif, 'Fraunces', serif);
  font-size: 2rem;
  margin: 0 0 0.5rem 0;
  color: var(--ink, #1F2421);
}

.name-en {
  font-size: 1.1rem;
  color: #666;
  margin: 0 0 1.5rem 0;
}

.video-player {
  margin: 1.5rem 0;
  background: #000;
  border-radius: 12px;
  overflow: hidden;
}

.video-player video {
  width: 100%;
  display: block;
}

.detail-tags {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

.detail-section {
  margin-bottom: 1.5rem;
}

.detail-section h3 {
  font-size: 1.3rem;
  margin: 0 0 0.75rem 0;
  color: var(--terracotta, #C4612F);
}

.detail-section p {
  line-height: 1.6;
  color: #333;
}

.detail-section ol,
.detail-section ul {
  padding-left: 1.5rem;
  line-height: 1.8;
}

.detail-section li {
  margin-bottom: 0.5rem;
}

.detail-actions {
  display: flex;
  gap: 1rem;
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid #eee;
}

.btn-favorite {
  padding: 0.75rem 2rem;
  background: white;
  border: 2px solid var(--terracotta, #C4612F);
  border-radius: 8px;
  color: var(--terracotta, #C4612F);
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-favorite:hover {
  background: var(--terracotta, #C4612F);
  color: white;
}

.btn-favorite.favorited {
  background: var(--terracotta, #C4612F);
  color: white;
}

@media (max-width: 768px) {
  .exercise-library {
    padding: 1rem;
  }

  .header-content {
    flex-direction: column;
    align-items: stretch;
  }

  .search-box {
    max-width: 100%;
  }

  .filters {
    flex-direction: column;
  }

  .exercise-grid {
    grid-template-columns: 1fr;
  }

  .modal-content {
    padding: 1.5rem;
  }
}
</style>
