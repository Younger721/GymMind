<template>
  <div class="dashboard-view">
    <el-row :gutter="20">
      <!-- Summary Cards -->
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <el-icon :size="40" color="#409eff"><TrendCharts /></el-icon>
            <div class="stats-info">
              <div class="stats-value">{{ stats.totalWorkouts }}</div>
              <div class="stats-label">总训练次数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <el-icon :size="40" color="#67c23a"><Timer /></el-icon>
            <div class="stats-info">
              <div class="stats-value">{{ stats.totalMinutes }}</div>
              <div class="stats-label">总时长（分钟）</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <el-icon :size="40" color="#f56c6c"><Fire /></el-icon>
            <div class="stats-info">
              <div class="stats-value">{{ stats.totalCalories }}</div>
              <div class="stats-label">总消耗（卡）</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <el-icon :size="40" color="#e6a23c"><Trophy /></el-icon>
            <div class="stats-info">
              <div class="stats-value">{{ stats.streak }}</div>
              <div class="stats-label">连续打卡（天）</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <!-- Weight Trend Chart -->
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>📊 体重变化趋势</span>
              <el-select v-model="weightPeriod" size="small" style="width: 100px">
                <el-option label="7天" value="7" />
                <el-option label="30天" value="30" />
                <el-option label="90天" value="90" />
              </el-select>
            </div>
          </template>
          <div ref="weightChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>

      <!-- Workout Distribution -->
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header>
            <span>🎯 训练分布</span>
          </template>
          <div ref="workoutDistChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <!-- Weekly Heatmap -->
      <el-col :xs="24" :md="16">
        <el-card>
          <template #header>
            <span>🔥 训练热力图</span>
          </template>
          <div ref="heatmapChartRef" style="height: 250px"></div>
        </el-card>
      </el-col>

      <!-- Muscle Group Radar -->
      <el-col :xs="24" :md="8">
        <el-card>
          <template #header>
            <span>💪 肌群平衡</span>
          </template>
          <div ref="radarChartRef" style="height: 250px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <!-- Nutrition Trend -->
      <el-col :xs="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>🍎 营养摄入趋势</span>
              <el-radio-group v-model="nutritionView" size="small">
                <el-radio-button value="calories">卡路里</el-radio-button>
                <el-radio-button value="macros">三大营养素</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="nutritionChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <!-- Body Metrics Progress -->
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header>
            <span>📐 身体数据进度</span>
          </template>
          <div class="body-metrics">
            <div class="metric-item">
              <span class="metric-label">体重目标</span>
              <el-progress :percentage="calculateProgress(currentWeight, targetWeight, initialWeight)" />
              <span class="metric-value">{{ currentWeight }}kg / {{ targetWeight }}kg</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">体脂目标</span>
              <el-progress :percentage="calculateProgress(currentBodyFat, targetBodyFat, initialBodyFat)" color="#67c23a" />
              <span class="metric-value">{{ currentBodyFat }}% / {{ targetBodyFat }}%</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- Recent Activities -->
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header>
            <span>📝 最近活动</span>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="activity in recentActivities"
              :key="activity.id"
              :timestamp="activity.date"
              placement="top"
            >
              <div class="activity-item">
                <el-icon><Trophy /></el-icon>
                <span>{{ activity.description }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import * as echarts from 'echarts'

// Chart refs
const weightChartRef = ref<HTMLElement>()
const workoutDistChartRef = ref<HTMLElement>()
const heatmapChartRef = ref<HTMLElement>()
const radarChartRef = ref<HTMLElement>()
const nutritionChartRef = ref<HTMLElement>()

// Data
const weightPeriod = ref('30')
const nutritionView = ref('calories')

const stats = ref({
  totalWorkouts: 156,
  totalMinutes: 7820,
  totalCalories: 45680,
  streak: 12
})

const currentWeight = ref(75.5)
const targetWeight = ref(72)
const initialWeight = ref(78)

const currentBodyFat = ref(18)
const targetBodyFat = ref(15)
const initialBodyFat = ref(22)

const recentActivities = ref([
  { id: 1, date: '2026-09-10 18:30', description: '完成胸部训练，消耗 420 卡' },
  { id: 2, date: '2026-09-09 19:00', description: '完成腿部训练，消耗 520 卡' },
  { id: 3, date: '2026-09-08 07:00', description: '完成有氧训练，消耗 380 卡' },
  { id: 4, date: '2026-09-07 18:30', description: '完成背部训练，消耗 450 卡' }
])

// Initialize charts
onMounted(() => {
  initWeightChart()
  initWorkoutDistChart()
  initHeatmapChart()
  initRadarChart()
  initNutritionChart()
})

watch(weightPeriod, () => {
  initWeightChart()
})

watch(nutritionView, () => {
  initNutritionChart()
})

const initWeightChart = () => {
  if (!weightChartRef.value) return

  const chart = echarts.init(weightChartRef.value)

  const dates = generateDates(parseInt(weightPeriod.value))
  const weights = generateWeightData(dates.length)

  const option = {
    tooltip: {
      trigger: 'axis'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates
    },
    yAxis: {
      type: 'value',
      name: 'kg',
      min: 72,
      max: 79
    },
    series: [
      {
        name: '体重',
        type: 'line',
        smooth: true,
        data: weights,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.5)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.1)' }
          ])
        },
        markLine: {
          data: [
            { type: 'average', name: '平均值' },
            { yAxis: targetWeight.value, name: '目标', lineStyle: { color: '#67c23a', type: 'dashed' } }
          ]
        }
      }
    ]
  }

  chart.setOption(option)
  window.addEventListener('resize', () => chart.resize())
}

const initWorkoutDistChart = () => {
  if (!workoutDistChartRef.value) return

  const chart = echarts.init(workoutDistChartRef.value)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} 次 ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '训练类型',
        type: 'pie',
        radius: '60%',
        data: [
          { value: 45, name: '力量训练' },
          { value: 30, name: '有氧训练' },
          { value: 15, name: '拉伸放松' },
          { value: 10, name: '核心训练' }
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }

  chart.setOption(option)
  window.addEventListener('resize', () => chart.resize())
}

const initHeatmapChart = () => {
  if (!heatmapChartRef.value) return

  const chart = echarts.init(heatmapChartRef.value)

  const hours = ['00', '02', '04', '06', '08', '10', '12', '14', '16', '18', '20', '22']
  const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

  const data = []
  for (let i = 0; i < days.length; i++) {
    for (let j = 0; j < hours.length; j++) {
      data.push([j, i, Math.floor(Math.random() * 5)])
    }
  }

  const option = {
    tooltip: {
      position: 'top'
    },
    grid: {
      height: '70%',
      top: '5%'
    },
    xAxis: {
      type: 'category',
      data: hours,
      splitArea: {
        show: true
      }
    },
    yAxis: {
      type: 'category',
      data: days,
      splitArea: {
        show: true
      }
    },
    visualMap: {
      min: 0,
      max: 5,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '5%',
      inRange: {
        color: ['#f0f0f0', '#409eff', '#67c23a', '#e6a23c', '#f56c6c']
      }
    },
    series: [
      {
        name: '训练强度',
        type: 'heatmap',
        data: data,
        label: {
          show: false
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }

  chart.setOption(option)
  window.addEventListener('resize', () => chart.resize())
}

const initRadarChart = () => {
  if (!radarChartRef.value) return

  const chart = echarts.init(radarChartRef.value)

  const option = {
    tooltip: {},
    radar: {
      indicator: [
        { name: '胸部', max: 100 },
        { name: '背部', max: 100 },
        { name: '腿部', max: 100 },
        { name: '肩部', max: 100 },
        { name: '手臂', max: 100 },
        { name: '核心', max: 100 }
      ]
    },
    series: [
      {
        name: '训练量',
        type: 'radar',
        data: [
          {
            value: [85, 90, 78, 82, 75, 88],
            name: '本周',
            areaStyle: {
              color: 'rgba(64, 158, 255, 0.3)'
            }
          },
          {
            value: [78, 85, 70, 75, 70, 80],
            name: '上周',
            areaStyle: {
              color: 'rgba(103, 194, 58, 0.2)'
            }
          }
        ]
      }
    ]
  }

  chart.setOption(option)
  window.addEventListener('resize', () => chart.resize())
}

const initNutritionChart = () => {
  if (!nutritionChartRef.value) return

  const chart = echarts.init(nutritionChartRef.value)

  const dates = generateDates(14)

  let option

  if (nutritionView.value === 'calories') {
    option = {
      tooltip: {
        trigger: 'axis'
      },
      legend: {
        data: ['摄入', '消耗', '目标']
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: dates
      },
      yAxis: {
        type: 'value',
        name: 'kcal'
      },
      series: [
        {
          name: '摄入',
          type: 'bar',
          data: dates.map(() => 2000 + Math.random() * 500)
        },
        {
          name: '消耗',
          type: 'bar',
          data: dates.map(() => 2200 + Math.random() * 300)
        },
        {
          name: '目标',
          type: 'line',
          data: dates.map(() => 2300),
          lineStyle: { type: 'dashed' }
        }
      ]
    }
  } else {
    option = {
      tooltip: {
        trigger: 'axis'
      },
      legend: {
        data: ['蛋白质', '碳水化合物', '脂肪']
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: dates
      },
      yAxis: {
        type: 'value',
        name: 'g'
      },
      series: [
        {
          name: '蛋白质',
          type: 'line',
          stack: 'Total',
          smooth: true,
          areaStyle: {},
          data: dates.map(() => 120 + Math.random() * 40)
        },
        {
          name: '碳水化合物',
          type: 'line',
          stack: 'Total',
          smooth: true,
          areaStyle: {},
          data: dates.map(() => 250 + Math.random() * 50)
        },
        {
          name: '脂肪',
          type: 'line',
          stack: 'Total',
          smooth: true,
          areaStyle: {},
          data: dates.map(() => 60 + Math.random() * 20)
        }
      ]
    }
  }

  chart.setOption(option)
  window.addEventListener('resize', () => chart.resize())
}

const generateDates = (days: number) => {
  const dates = []
  const today = new Date()
  for (let i = days - 1; i >= 0; i--) {
    const date = new Date(today)
    date.setDate(date.getDate() - i)
    dates.push(`${date.getMonth() + 1}/${date.getDate()}`)
  }
  return dates
}

const generateWeightData = (length: number) => {
  const data = []
  let weight = initialWeight.value
  const targetDiff = (targetWeight.value - initialWeight.value) / length

  for (let i = 0; i < length; i++) {
    weight += targetDiff + (Math.random() - 0.5) * 0.5
    data.push(parseFloat(weight.toFixed(1)))
  }

  return data
}

const calculateProgress = (current: number, target: number, initial: number) => {
  if (initial === target) return 100
  const progress = ((initial - current) / (initial - target)) * 100
  return Math.min(100, Math.max(0, Math.round(progress)))
}
</script>

<style scoped>
.dashboard-view {
  padding: 20px;
}

.stats-card {
  margin-bottom: 20px;
}

.stats-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stats-info {
  flex: 1;
}

.stats-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  line-height: 1.2;
}

.stats-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.body-metrics {
  padding: 10px 0;
}

.metric-item {
  margin-bottom: 24px;
}

.metric-label {
  display: block;
  margin-bottom: 8px;
  color: #606266;
  font-weight: 500;
}

.metric-value {
  display: block;
  margin-top: 8px;
  color: #909399;
  font-size: 14px;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
}
</style>
