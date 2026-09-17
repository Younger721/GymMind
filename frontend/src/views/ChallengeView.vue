<template>
  <div class="challenge-container">
    <!-- Header -->
    <div class="challenge-header">
      <h2>健身挑战</h2>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>
        创建挑战
      </el-button>
    </div>

    <!-- Tabs -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="活跃挑战" name="active"></el-tab-pane>
      <el-tab-pane label="我的挑战" name="my"></el-tab-pane>
    </el-tabs>

    <!-- Challenges List -->
    <div class="challenges-list" v-loading="loading">
      <el-row :gutter="20">
        <el-col
          v-for="challenge in challenges"
          :key="challenge.id"
          :xs="24"
          :sm="12"
          :md="8"
        >
          <el-card class="challenge-card" shadow="hover">
            <!-- Challenge Image -->
            <div class="challenge-image">
              <img
                :src="challenge.imageUrl || '/default-challenge.jpg'"
                alt="challenge"
              />
              <el-tag
                :type="getDifficultyType(challenge.difficulty)"
                class="difficulty-tag"
              >
                {{ getDifficultyLabel(challenge.difficulty) }}
              </el-tag>
            </div>

            <!-- Challenge Info -->
            <div class="challenge-info">
              <h3>{{ challenge.name }}</h3>
              <p class="description">{{ challenge.description }}</p>

              <div class="challenge-details">
                <div class="detail-item">
                  <el-icon><Flag /></el-icon>
                  <span>{{ getGoalTypeLabel(challenge.goalType) }}</span>
                </div>
                <div class="detail-item">
                  <el-icon><TrophyBase /></el-icon>
                  <span>目标: {{ challenge.goalValue }}</span>
                </div>
                <div class="detail-item">
                  <el-icon><Calendar /></el-icon>
                  <span>{{ formatDate(challenge.startDate) }} - {{ formatDate(challenge.endDate) }}</span>
                </div>
                <div class="detail-item">
                  <el-icon><User /></el-icon>
                  <span>{{ challenge.participantCount }} 人参与</span>
                </div>
              </div>

              <!-- Progress Bar (if participating) -->
              <div v-if="challenge.isParticipating && challenge.userProgress !== undefined" class="progress-section">
                <div class="progress-info">
                  <span>我的进度</span>
                  <span class="progress-value">{{ challenge.userProgress }} / {{ challenge.goalValue }}</span>
                </div>
                <el-progress
                  :percentage="Math.min(100, (challenge.userProgress / challenge.goalValue) * 100)"
                  :status="challenge.userProgress >= challenge.goalValue ? 'success' : undefined"
                />
              </div>

              <!-- Actions -->
              <div class="challenge-actions">
                <el-button
                  v-if="!challenge.isParticipating"
                  type="primary"
                  @click="handleJoinChallenge(challenge)"
                  :loading="joiningChallengeId === challenge.id"
                >
                  加入挑战
                </el-button>
                <template v-else>
                  <el-button @click="handleViewLeaderboard(challenge)">
                    查看排行榜
                  </el-button>
                  <el-button type="primary" @click="handleUpdateProgress(challenge)">
                    更新进度
                  </el-button>
                </template>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-empty v-if="!loading && challenges.length === 0" description="暂无挑战" />
    </div>

    <!-- Create Challenge Dialog -->
    <el-dialog
      v-model="showCreateDialog"
      title="创建挑战"
      width="600px"
    >
      <el-form :model="newChallenge" label-width="100px">
        <el-form-item label="挑战名称">
          <el-input v-model="newChallenge.name" placeholder="例如: 30天深蹲挑战" />
        </el-form-item>

        <el-form-item label="挑战描述">
          <el-input
            v-model="newChallenge.description"
            type="textarea"
            :rows="3"
            placeholder="描述挑战的目标和规则..."
          />
        </el-form-item>

        <el-form-item label="目标类型">
          <el-select v-model="newChallenge.goalType" placeholder="选择目标类型">
            <el-option label="训练次数" value="WORKOUT_COUNT"></el-option>
            <el-option label="累计时长(分钟)" value="TOTAL_TIME"></el-option>
            <el-option label="累计消耗(卡路里)" value="TOTAL_CALORIES"></el-option>
            <el-option label="累计距离(公里)" value="DISTANCE"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="目标值">
          <el-input-number
            v-model="newChallenge.goalValue"
            :min="1"
            :max="10000"
          />
        </el-form-item>

        <el-form-item label="难度">
          <el-radio-group v-model="newChallenge.difficulty">
            <el-radio label="EASY">简单</el-radio>
            <el-radio label="MEDIUM">中等</el-radio>
            <el-radio label="HARD">困难</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="开始日期">
          <el-date-picker
            v-model="newChallenge.startDate"
            type="date"
            placeholder="选择开始日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>

        <el-form-item label="结束日期">
          <el-date-picker
            v-model="newChallenge.endDate"
            type="date"
            placeholder="选择结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateChallenge" :loading="creating">
          创建
        </el-button>
      </template>
    </el-dialog>

    <!-- Leaderboard Dialog -->
    <el-dialog
      v-model="showLeaderboardDialog"
      title="排行榜"
      width="500px"
    >
      <div v-loading="loadingLeaderboard" class="leaderboard">
        <div
          v-for="(participant, index) in leaderboard"
          :key="participant.userId"
          class="leaderboard-item"
          :class="{ 'my-rank': participant.userId === currentUserId }"
        >
          <div class="rank">
            <el-icon v-if="index === 0" color="#FFD700"><Medal /></el-icon>
            <el-icon v-else-if="index === 1" color="#C0C0C0"><Medal /></el-icon>
            <el-icon v-else-if="index === 2" color="#CD7F32"><Medal /></el-icon>
            <span v-else class="rank-number">{{ participant.rank }}</span>
          </div>
          <div class="participant-info">
            <span class="username">{{ participant.username }}</span>
            <span class="progress-text">进度: {{ participant.progress }}</span>
          </div>
          <el-tag
            v-if="participant.status === 'COMPLETED'"
            type="success"
            size="small"
          >
            已完成
          </el-tag>
        </div>
        <el-empty v-if="!loadingLeaderboard && leaderboard.length === 0" description="暂无参与者" />
      </div>
    </el-dialog>

    <!-- Update Progress Dialog -->
    <el-dialog
      v-model="showProgressDialog"
      title="更新进度"
      width="400px"
    >
      <div class="progress-update">
        <p>当前进度: {{ selectedChallenge?.userProgress || 0 }}</p>
        <el-input-number
          v-model="newProgress"
          :min="0"
          :max="selectedChallenge?.goalValue || 1000"
          style="width: 100%"
        />
        <p class="hint">输入你的最新进度值</p>
      </div>

      <template #footer>
        <el-button @click="showProgressDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitProgress" :loading="updatingProgress">
          更新
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Plus,
  Flag,
  TrophyBase,
  Calendar,
  User,
  Medal
} from '@element-plus/icons-vue'
import {
  getActiveChallenges,
  getUserChallenges,
  createChallenge,
  joinChallenge,
  updateProgress,
  getLeaderboard,
  type ChallengeResponse,
  type ParticipantResponse
} from '@/api/challenge'

const activeTab = ref('active')
const challenges = ref<ChallengeResponse[]>([])
const loading = ref(false)
const currentUserId = ref(1) // Replace with actual user ID from auth store

// Create challenge
const showCreateDialog = ref(false)
const creating = ref(false)
const newChallenge = ref({
  name: '',
  description: '',
  goalType: 'WORKOUT_COUNT',
  goalValue: 30,
  difficulty: 'MEDIUM',
  startDate: '',
  endDate: ''
})

// Join challenge
const joiningChallengeId = ref<number | null>(null)

// Leaderboard
const showLeaderboardDialog = ref(false)
const loadingLeaderboard = ref(false)
const leaderboard = ref<ParticipantResponse[]>([])

// Update progress
const showProgressDialog = ref(false)
const selectedChallenge = ref<ChallengeResponse | null>(null)
const newProgress = ref(0)
const updatingProgress = ref(false)

onMounted(() => {
  loadChallenges()
})

const handleTabChange = () => {
  loadChallenges()
}

const loadChallenges = async () => {
  loading.value = true
  try {
    if (activeTab.value === 'active') {
      challenges.value = await getActiveChallenges()
    } else {
      challenges.value = await getUserChallenges()
    }
  } catch (error) {
    ElMessage.error('加载挑战失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleCreateChallenge = async () => {
  if (!newChallenge.value.name || !newChallenge.value.startDate || !newChallenge.value.endDate) {
    ElMessage.warning('请填写完整信息')
    return
  }

  creating.value = true
  try {
    await createChallenge(newChallenge.value as any)
    ElMessage.success('创建成功')
    showCreateDialog.value = false

    // Reset form
    newChallenge.value = {
      name: '',
      description: '',
      goalType: 'WORKOUT_COUNT',
      goalValue: 30,
      difficulty: 'MEDIUM',
      startDate: '',
      endDate: ''
    }

    loadChallenges()
  } catch (error) {
    ElMessage.error('创建失败')
    console.error(error)
  } finally {
    creating.value = false
  }
}

const handleJoinChallenge = async (challenge: ChallengeResponse) => {
  joiningChallengeId.value = challenge.id
  try {
    await joinChallenge(challenge.id)
    ElMessage.success('加入成功')
    loadChallenges()
  } catch (error) {
    ElMessage.error('加入失败')
    console.error(error)
  } finally {
    joiningChallengeId.value = null
  }
}

const handleViewLeaderboard = async (challenge: ChallengeResponse) => {
  selectedChallenge.value = challenge
  showLeaderboardDialog.value = true
  loadingLeaderboard.value = true

  try {
    leaderboard.value = await getLeaderboard(challenge.id)
  } catch (error) {
    ElMessage.error('加载排行榜失败')
    console.error(error)
  } finally {
    loadingLeaderboard.value = false
  }
}

const handleUpdateProgress = (challenge: ChallengeResponse) => {
  selectedChallenge.value = challenge
  newProgress.value = challenge.userProgress || 0
  showProgressDialog.value = true
}

const handleSubmitProgress = async () => {
  if (!selectedChallenge.value) return

  updatingProgress.value = true
  try {
    await updateProgress(selectedChallenge.value.id, newProgress.value)
    ElMessage.success('进度更新成功')
    showProgressDialog.value = false
    loadChallenges()
  } catch (error) {
    ElMessage.error('更新失败')
    console.error(error)
  } finally {
    updatingProgress.value = false
  }
}

const getGoalTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    WORKOUT_COUNT: '训练次数',
    TOTAL_TIME: '累计时长',
    TOTAL_CALORIES: '累计消耗',
    DISTANCE: '累计距离'
  }
  return labels[type] || type
}

const getDifficultyLabel = (difficulty?: string) => {
  const labels: Record<string, string> = {
    EASY: '简单',
    MEDIUM: '中等',
    HARD: '困难'
  }
  return labels[difficulty || 'MEDIUM'] || '中等'
}

const getDifficultyType = (difficulty?: string) => {
  const types: Record<string, any> = {
    EASY: 'success',
    MEDIUM: 'warning',
    HARD: 'danger'
  }
  return types[difficulty || 'MEDIUM'] || 'warning'
}

const formatDate = (date: string) => {
  return new Date(date).toLocaleDateString('zh-CN', {
    month: 'numeric',
    day: 'numeric'
  })
}
</script>

<style scoped lang="scss">
.challenge-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.challenge-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  h2 {
    margin: 0;
  }
}

.challenges-list {
  margin-top: 20px;

  .challenge-card {
    margin-bottom: 20px;
    height: 100%;

    .challenge-image {
      position: relative;
      height: 180px;
      margin: -20px -20px 15px -20px;
      overflow: hidden;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      .difficulty-tag {
        position: absolute;
        top: 10px;
        right: 10px;
      }
    }

    .challenge-info {
      h3 {
        margin: 0 0 10px 0;
        font-size: 18px;
      }

      .description {
        color: #606266;
        font-size: 14px;
        margin-bottom: 15px;
        line-height: 1.6;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .challenge-details {
        margin-bottom: 15px;

        .detail-item {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 8px;
          font-size: 14px;
          color: #606266;

          .el-icon {
            color: #909399;
          }
        }
      }

      .progress-section {
        margin: 15px 0;
        padding: 10px;
        background: #f5f7fa;
        border-radius: 4px;

        .progress-info {
          display: flex;
          justify-content: space-between;
          margin-bottom: 8px;
          font-size: 14px;

          .progress-value {
            font-weight: 600;
            color: #409eff;
          }
        }
      }

      .challenge-actions {
        display: flex;
        gap: 10px;
        margin-top: 15px;

        .el-button {
          flex: 1;
        }
      }
    }
  }
}

.leaderboard {
  .leaderboard-item {
    display: flex;
    align-items: center;
    gap: 15px;
    padding: 15px;
    border-bottom: 1px solid #ebeef5;
    transition: background 0.3s;

    &:hover {
      background: #f5f7fa;
    }

    &.my-rank {
      background: #ecf5ff;
    }

    .rank {
      width: 40px;
      text-align: center;

      .el-icon {
        font-size: 24px;
      }

      .rank-number {
        font-size: 18px;
        font-weight: 600;
        color: #606266;
      }
    }

    .participant-info {
      flex: 1;
      display: flex;
      flex-direction: column;

      .username {
        font-weight: 600;
        margin-bottom: 5px;
      }

      .progress-text {
        font-size: 13px;
        color: #909399;
      }
    }
  }
}

.progress-update {
  text-align: center;

  p {
    margin: 10px 0;
  }

  .hint {
    font-size: 13px;
    color: #909399;
  }
}
</style>
