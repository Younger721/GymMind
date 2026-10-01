<template>
  <div class="check-in-page">
    <h2 class="simple-page-title">签到管理</h2>
    <p class="simple-page-desc">前台扫描会员固定二维码，登记普通到店或课程签到。</p>
    <div class="simple-card scan-card">
      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="二维码内容">
          <el-input ref="tokenInput" v-model="token" autofocus placeholder="请使用扫码枪扫描，或粘贴二维码 token" @keyup.enter="submit" />
        </el-form-item>
        <el-form-item label="课程 ID（可选）">
          <el-input v-model="courseId" placeholder="留空表示普通到店签到" />
        </el-form-item>
        <el-button type="primary" :loading="loading" :disabled="!token.trim()" @click="submit">确认签到</el-button>
        <el-button :type="cameraRunning ? 'danger' : 'default'" @click="toggleCamera">{{ cameraRunning ? '停止摄像头' : '摄像头扫码' }}</el-button>
      </el-form>
      <video v-show="cameraRunning" ref="video" class="camera" autoplay muted playsinline />
      <el-alert v-if="message" :title="message" :type="messageType" show-icon class="result" />
    </div>
    <div class="simple-card rules">
      <h3>签到规则</h3>
      <p>不填写课程 ID：记录普通到店签到。</p>
      <p>填写课程 ID：校验预约后记录课程参与，并同步记录到店签到。</p>
      <p>账号或会员状态异常时，固定二维码不能使用。</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { checkinsApi } from '@/api/checkins'

const token = ref('')
const courseId = ref('')
const loading = ref(false)
const message = ref('')
const messageType = ref<'success' | 'error'>('success')
const tokenInput = ref()
const video = ref<HTMLVideoElement>()
const cameraRunning = ref(false)
let stream: MediaStream | null = null
let scanTimer = 0

type Detector = { detect(video: HTMLVideoElement): Promise<Array<{ rawValue: string }>> }
const getDetector = () => {
  const BarcodeDetectorCtor = (window as unknown as { BarcodeDetector?: new (options?: { formats: string[] }) => Detector }).BarcodeDetector
  return BarcodeDetectorCtor ? new BarcodeDetectorCtor({ formats: ['qr_code'] }) : null
}

async function toggleCamera() {
  if (cameraRunning.value) { stopCamera(); return }
  const detector = getDetector()
  if (!detector || !navigator.mediaDevices?.getUserMedia) {
    ElMessage.warning('当前浏览器不支持摄像头二维码识别，请使用扫码枪或粘贴 token')
    return
  }
  try {
    stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } })
    cameraRunning.value = true
    await nextTick()
    if (video.value) video.value.srcObject = stream
    scanTimer = window.setInterval(async () => {
      if (!cameraRunning.value || !video.value) return
      try {
        const codes = await detector.detect(video.value)
        const value = codes[0]?.rawValue?.trim()
        if (value) { token.value = value; stopCamera(); await submit() }
      } catch { /* 摄像头尚未准备好时继续扫描 */ }
    }, 400)
  } catch {
    ElMessage.error('无法打开摄像头，请检查浏览器权限')
  }
}

function stopCamera() {
  cameraRunning.value = false
  if (scanTimer) window.clearInterval(scanTimer)
  scanTimer = 0
  stream?.getTracks().forEach(track => track.stop())
  stream = null
  if (video.value) video.value.srcObject = null
}

async function submit() {
  if (!token.value.trim() || loading.value) return
  loading.value = true; message.value = ''
  try {
    await checkinsApi.scan(token.value.trim(), courseId.value.trim() ? Number(courseId.value) : undefined)
    messageType.value = 'success'
    message.value = courseId.value.trim() ? '课程签到成功，已记录到店。' : '到店签到成功。'
    token.value = ''; courseId.value = ''
    await nextTick(); tokenInput.value?.focus?.()
  } catch (error: any) {
    messageType.value = 'error'
    message.value = error?.response?.data?.message || '签到失败，请确认会员状态和预约信息。'
  } finally { loading.value = false }
}

onBeforeUnmount(stopCamera)
</script>

<style scoped>
.check-in-page { max-width: 720px; margin: 0 auto; }
.scan-card { max-width: 560px; }
.result { margin-top: 16px; }
.rules { margin-top: 16px; color: var(--text-muted); }
.rules h3 { color: var(--text); margin-top: 0; }
.rules p { margin: 8px 0; }
.camera { width: min(100%, 420px); margin-top: 16px; border-radius: 8px; background: #111; aspect-ratio: 4 / 3; object-fit: cover; }
</style>
