<template>
  <div>
    <h2 class="simple-page-title">会籍与支付</h2>
    <p class="simple-page-desc">会员卡套餐、发卡、订单与收款</p>

    <el-tabs v-model="tab">
      <el-tab-pane label="套餐" name="packages">
        <BizCrudPanel title="创建套餐" :create-fields="packageFields" :loader="emptyLoader" :creator="createPackage" />
        <div class="simple-card lookup-box">
          <el-form inline>
            <el-form-item label="套餐 ID"><el-input v-model="packageId" style="width: 120px" /></el-form-item>
            <el-button :loading="lookupLoading" @click="findPackage">查询</el-button>
          </el-form>
          <JsonBlock v-if="packageResult" :data="packageResult" />
        </div>
      </el-tab-pane>
      <el-tab-pane label="发卡" name="grant">
        <BizCrudPanel title="发放会籍" :create-fields="grantFields" :loader="emptyLoader" :creator="grantMembership" />
      </el-tab-pane>
      <el-tab-pane label="订单" name="orders">
        <BizCrudPanel title="创建订单" :create-fields="orderFields" :loader="emptyLoader" :creator="createOrder" />
      </el-tab-pane>
      <el-tab-pane label="收款" name="payments">
        <BizCrudPanel title="记录支付" :create-fields="paymentFields" :loader="emptyLoader" :creator="recordPayment" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import BizCrudPanel, { type FieldDef } from '@/components/business/BizCrudPanel.vue'
import JsonBlock from '@/components/business/JsonBlock.vue'
import { membershipApi } from '@/api/membership'

const tab = ref('packages')
const packageId = ref('')
const packageResult = ref<unknown>(null)
const lookupLoading = ref(false)

const packageFields: FieldDef[] = [
  { key: 'name', label: '套餐名' },
  { key: 'entitlementType', label: '权益类型', placeholder: 'UNLIMITED' },
  { key: 'validDays', label: '有效天数', type: 'number' },
  { key: 'price', label: '价格', placeholder: '999.00' },
  { key: 'currency', label: '币种', placeholder: 'CNY' }
]

const grantFields: FieldDef[] = [
  { key: 'memberId', label: '会员ID', type: 'number' },
  { key: 'packageId', label: '套餐ID', type: 'number' },
  { key: 'startsAt', label: '开始', placeholder: '2026-09-17T00:00:00Z' },
  { key: 'expiresAt', label: '到期', placeholder: '2026-12-17T00:00:00Z' }
]

const orderFields: FieldDef[] = [
  { key: 'memberId', label: '会员ID', type: 'number' },
  { key: 'total', label: '总额', placeholder: '999.00' },
  { key: 'currency', label: '币种', placeholder: 'CNY' }
]

const paymentFields: FieldDef[] = [
  { key: 'orderId', label: '订单ID', type: 'number' },
  { key: 'amount', label: '金额', placeholder: '999.00' },
  { key: 'currency', label: '币种', placeholder: 'CNY' },
  { key: 'reference', label: '流水号' },
  { key: 'paymentMethod', label: '方式', placeholder: 'WECHAT' }
]

async function emptyLoader() {
  return { data: [] }
}

async function createPackage(p: Record<string, string>) {
  return membershipApi.createPackage({
    name: p.name,
    entitlementType: p.entitlementType,
    entitlementCount: null,
    validDays: Number(p.validDays),
    price: p.price,
    currency: p.currency
  })
}

async function findPackage() {
  if (!packageId.value) return ElMessage.warning('请输入套餐 ID')
  lookupLoading.value = true
  try {
    packageResult.value = await membershipApi.findPackage(Number(packageId.value))
  } finally {
    lookupLoading.value = false
  }
}

async function grantMembership(p: Record<string, string>) {
  return membershipApi.grantMembership({
    memberId: Number(p.memberId),
    packageId: Number(p.packageId),
    startsAt: p.startsAt,
    expiresAt: p.expiresAt
  })
}

async function createOrder(p: Record<string, string>) {
  return membershipApi.createOrder({
    memberId: Number(p.memberId),
    total: p.total,
    currency: p.currency
  })
}

async function recordPayment(p: Record<string, string>) {
  return membershipApi.recordPayment({
    orderId: Number(p.orderId),
    amount: p.amount,
    currency: p.currency,
    reference: p.reference,
    paymentMethod: p.paymentMethod
  })
}
</script>

<style scoped>
.lookup-box {
  margin-top: 16px;
}
</style>
