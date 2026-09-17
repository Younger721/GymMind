<template>
  <div class="weekly-report">
    <div class="header">
      <h1>Weekly Report</h1>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="to"
        start-placeholder="Start date"
        end-placeholder="End date"
        @change="loadReport"
      />
    </div>

    <el-skeleton :loading="loading" animated :count="3">
      <template #default>
        <div v-if="report">
          <!-- AI分析 -->
          <el-card class="analysis-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span>AI Analysis</span>
                <el-icon><ChatDotRound /></el-icon>
              </div>
            </template>
            <div class="analysis-content">
              <p>{{ report.aiAnalysis }}</p>
            </div>
          </el-card>

          <!-- 训练总结 -->
          <el-row :gutter="20" style="margin-top: 20px">
            <el-col :span="12">
              <el-card>
                <template #header>
                  <span>Workout Summary</span>
                </template>
                <div class="summary-stats">
                  <div class="stat-item">
                    <span class="stat-label">Training Days</span>
                    <span class="stat-value">{{ report.workoutSummary.totalDays }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">Total Workouts</span>
                    <span class="stat-value">{{ report.workoutSummary.totalWorkouts }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">Total Volume</span>
                    <span class="stat-value">{{ formatNumber(report.workoutSummary.totalVolume) }} kg</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">Avg Volume/Day</span>
                    <span class="stat-value">{{ formatNumber(report.workoutSummary.avgVolumePerDay) }} kg</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">Most Frequent</span>
                    <span class="stat-value">{{ report.workoutSummary.mostFrequentExercise }}</span>
                  </div>
                </div>

                <!-- 个人记录 -->
                <el-divider />
                <h3>Personal Records</h3>
                <el-table :data="report.workoutSummary.personalRecords" size="small">
                  <el-table-column prop="exerciseName" label="Exercise" />
                  <el-table-column label="Weight">
                    <template #default="{ row }">{{ row.weight }} kg × {{ row.reps }}</template>
                  </el-table-column>
                  <el-table-column prop="achievedDate" label="Date" width="100" />
                </el-table>
              </el-card>
            </el-col>

            <!-- 营养总结 -->
            <el-col :span="12">
              <el-card>
                <template #header>
                  <span>Nutrition Summary</span>
                </template>
                <div class="summary-stats">
                  <div class="stat-item">
                    <span class="stat-label">Avg Calories</span>
                    <span class="stat-value">
                      {{ formatNumber(report.nutritionSummary.avgCalories) }} /
                      {{ formatNumber(report.nutritionSummary.calorieTarget) }} kcal
                    </span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">Avg Protein</span>
                    <span class="stat-value">
                      {{ formatNumber(report.nutritionSummary.avgProtein) }} /
                      {{ formatNumber(report.nutritionSummary.proteinTarget) }} g
                    </span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">Avg Carbs</span>
                    <span class="stat-value">{{ formatNumber(report.nutritionSummary.avgCarbs) }} g</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">Avg Fats</span>
                    <span class="stat-value">{{ formatNumber(report.nutritionSummary.avgFats) }} g</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">Days on Target</span>
                    <span class="stat-value">
                      {{ report.nutritionSummary.daysOnTarget }} / {{ report.nutritionSummary.totalDays }}
                    </span>
                  </div>
                </div>

                <!-- 进度条 -->
                <el-divider />
                <div class="progress-item">
                  <span>Calorie Accuracy</span>
                  <el-progress
                    :percentage="getCalorieAccuracy()"
                    :color="getProgressColor(getCalorieAccuracy())"
                  />
                </div>
                <div class="progress-item">
                  <span>Protein Accuracy</span>
                  <el-progress
                    :percentage="getProteinAccuracy()"
                    :color="getProgressColor(getProteinAccuracy())"
                  />
                </div>
              </el-card>
            </el-col>
          </el-row>

          <!-- 改进建议 -->
          <el-card style="margin-top: 20px">
            <template #header>
              <span>Suggestions for Improvement</span>
            </template>
            <el-alert
              v-for="(suggestion, index) in report.suggestions"
              :key="index"
              :title="suggestion"
              type="info"
              :closable="false"
              style="margin-bottom: 10px"
            />
            <el-empty v-if="!report.suggestions || report.suggestions.length === 0" description="Great job! Keep up the good work!" />
          </el-card>
        </div>

        <el-empty v-else description="No report data available" />
      </template>
    </el-skeleton>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getWeeklyReport, type WeeklyReport } from '@/api/report'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const report = ref<WeeklyReport | null>(null)
const dateRange = ref<[Date, Date]>([
  new Date(Date.now() - 6 * 24 * 60 * 60 * 1000),
  new Date()
])

const loadReport = async () => {
  if (!dateRange.value || dateRange.value.length !== 2) return

  try {
    loading.value = true
    const startDate = formatDate(dateRange.value[0])
    const endDate = formatDate(dateRange.value[1])
    report.value = await getWeeklyReport(startDate, endDate)
  } catch (error: any) {
    ElMessage.error(error.message || 'Failed to load report')
  } finally {
    loading.value = false
  }
}

const formatDate = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const formatNumber = (value: number) => {
  return Math.round(value)
}

const getCalorieAccuracy = () => {
  if (!report.value) return 0
  const { avgCalories, calorieTarget } = report.value.nutritionSummary
  if (calorieTarget === 0) return 0
  return Math.min(100, Math.round((avgCalories / calorieTarget) * 100))
}

const getProteinAccuracy = () => {
  if (!report.value) return 0
  const { avgProtein, proteinTarget } = report.value.nutritionSummary
  if (proteinTarget === 0) return 0
  return Math.min(100, Math.round((avgProtein / proteinTarget) * 100))
}

const getProgressColor = (percentage: number) => {
  if (percentage >= 90 && percentage <= 110) return '#67c23a'
  if (percentage >= 80 && percentage <= 120) return '#e6a23c'
  return '#f56c6c'
}

onMounted(() => {
  loadReport()
})
</script>

<style scoped>
.weekly-report {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h1 {
  margin: 0;
  color: #303133;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.analysis-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.analysis-card :deep(.el-card__header) {
  border-bottom-color: rgba(255, 255, 255, 0.2);
  color: white;
}

.analysis-content {
  font-size: 16px;
  line-height: 1.8;
  white-space: pre-wrap;
}

.summary-stats {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
}

.stat-label {
  font-size: 14px;
  color: #606266;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #409eff;
}

.progress-item {
  margin-bottom: 15px;
}

.progress-item span {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
}
</style>
