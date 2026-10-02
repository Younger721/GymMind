<template>
  <div class="equipment-page">
    <div class="page-head">
      <div>
        <h2 class="simple-page-title">器械管理</h2>
        <p class="simple-page-desc">管理健身房器械库存、申请与配送入库</p>
      </div>
      <div class="page-actions">
        <el-button v-if="isGymAdmin" type="primary" @click="openRequest">申请器械</el-button>
        <el-button v-if="isPlatformAdmin && activeTab === 'catalog'" type="primary" @click="openCatalog">新增器械</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-change="loadActiveTab">
      <el-tab-pane label="总览统计" name="overview" />
      <el-tab-pane v-if="isPlatformAdmin" label="器械目录" name="catalog" />
      <el-tab-pane label="库存" name="inventory" />
      <el-tab-pane label="申请记录" name="requests" />
      <el-tab-pane v-if="isPlatformAdmin" label="配送记录" name="deliveries" />
    </el-tabs>

    <div v-if="activeTab === 'overview'" class="overview-grid" v-loading="loading">
      <div class="stat-card"><span>健身房数</span><strong>{{ statistics?.tenantCount ?? 0 }}</strong></div>
      <div class="stat-card"><span>器械种类</span><strong>{{ statistics?.catalogCount ?? 0 }}</strong></div>
      <div class="stat-card"><span>已配送数量</span><strong>{{ statistics?.deliveredQuantity ?? 0 }}</strong></div>
      <div class="stat-card"><span>已入库数量</span><strong>{{ statistics?.receivedQuantity ?? 0 }}</strong></div>
      <div class="stat-card"><span>待审批申请</span><strong>{{ statistics?.pendingRequests ?? 0 }}</strong></div>
      <div class="stat-card"><span>待确认配送</span><strong>{{ statistics?.waitingReceive ?? 0 }}</strong></div>
      <template v-if="isPlatformAdmin">
        <div class="stat-card warehouse-stat"><span>平台仓库可配送</span><strong>{{ statistics?.warehouseQuantity ?? 0 }}</strong></div>
        <div class="stat-card warehouse-stat"><span>平台配送中</span><strong>{{ statistics?.inTransitQuantity ?? 0 }}</strong></div>
        <div class="stat-card warehouse-stat"><span>平台器械总量</span><strong>{{ statistics?.platformTotalQuantity ?? 0 }}</strong></div>
      </template>
      <div v-if="isPlatformAdmin" class="simple-card table-card overview-table">
        <div class="section-heading"><h3>平台仓库库存</h3><el-button text @click="loadStatistics">刷新</el-button></div>
        <el-table :data="statistics?.warehouseInventory ?? []" stripe>
          <el-table-column prop="name" label="器械名称" min-width="140" />
          <el-table-column prop="category" label="分类" width="120" />
          <el-table-column prop="unit" label="单位" width="80" />
          <el-table-column prop="warehouseQuantity" label="仓库可配送" width="120" />
          <el-table-column prop="inTransitQuantity" label="配送中" width="100" />
          <el-table-column prop="outboundQuantity" label="历史已配送" width="110" />
          <el-table-column prop="tenantReceivedQuantity" label="门店已入库" width="110" />
          <el-table-column prop="totalQuantity" label="平台总量" width="100" />
          <el-table-column label="操作" width="100"><template #default="{ row }"><el-button link type="primary" @click="openWarehouseAdjustment(row)">调整</el-button></template></el-table-column>
        </el-table>
        <el-empty v-if="!loading && !(statistics?.warehouseInventory?.length)" description="暂无平台仓库库存" :image-size="56" />
      </div>
      <div class="simple-card table-card overview-table">
        <div class="section-heading"><h3>库存分布</h3><el-button text @click="loadInventory">刷新</el-button></div>
        <el-table :data="statistics?.inventory ?? []" stripe><el-table-column prop="tenantName" label="健身房" min-width="160" /><el-table-column prop="catalogName" label="器械" min-width="160" /><el-table-column prop="quantity" label="已入库数量" width="120" /></el-table>
        <el-empty v-if="!loading && !(statistics?.inventory?.length)" description="暂无库存数据" :image-size="56" />
      </div>
    </div>

    <div v-if="activeTab === 'catalog' && isPlatformAdmin" class="simple-card table-card" v-loading="loading">
      <div class="toolbar"><el-switch v-model="activeOnly" active-text="仅显示启用" @change="loadCatalog" /><el-input v-model="catalogKeyword" clearable placeholder="搜索器械名称" style="width: 220px" /></div>
      <el-table :data="filteredCatalogs" stripe><el-table-column prop="name" label="器械名称" min-width="160" /><el-table-column prop="category" label="分类" width="130" /><el-table-column prop="unit" label="单位" width="90" /><el-table-column prop="description" label="说明" min-width="220" /><el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.active ? 'success' : 'info'">{{ row.active ? '启用' : '停用' }}</el-tag></template></el-table-column><el-table-column label="操作" width="100"><template #default="{ row }"><el-button link type="primary" @click="openCatalog(row)">编辑</el-button></template></el-table-column></el-table>
      <el-empty v-if="!loading && !filteredCatalogs.length" description="暂无器械目录" :image-size="56" />
    </div>

    <div v-if="activeTab === 'inventory'" class="simple-card table-card" v-loading="loading">
      <div class="toolbar"><el-input v-model="inventoryKeyword" clearable placeholder="搜索健身房或器械" style="width: 260px" /><el-button @click="loadInventory">刷新</el-button></div>
      <el-table :data="filteredInventory" stripe><el-table-column v-if="isPlatformAdmin" prop="tenantName" label="健身房" min-width="160" /><el-table-column prop="name" label="器械" min-width="150" /><el-table-column prop="category" label="分类" width="120" /><el-table-column prop="deliveredQuantity" label="已配送" width="100" /><el-table-column prop="receivedQuantity" label="已入库" width="100" /><el-table-column prop="usableQuantity" label="可用" width="100" /><el-table-column prop="abnormalQuantity" label="异常" width="100" /><el-table-column label="最近入库" min-width="170"><template #default="{ row }">{{ formatDate(row.lastReceivedAt) }}</template></el-table-column></el-table>
      <el-empty v-if="!loading && !filteredInventory.length" description="暂无库存" :image-size="56" />
    </div>

    <div v-if="activeTab === 'requests'" class="simple-card table-card" v-loading="loading">
      <div class="toolbar"><el-select v-model="requestStatus" clearable placeholder="全部状态" style="width: 160px" @change="loadRequests"><el-option v-for="option in statusOptions" :key="option.value" :label="option.label" :value="option.value" /></el-select><el-button @click="loadRequests">刷新</el-button></div>
      <el-table :data="requests" stripe><el-table-column prop="id" label="申请单" width="90" /><el-table-column v-if="isPlatformAdmin" prop="tenantName" label="健身房" min-width="150" /><el-table-column label="申请器械" min-width="220"><template #default="{ row }">{{ requestItemsLabel(row) }}</template></el-table-column><el-table-column prop="note" label="申请说明" min-width="180" /><el-table-column label="状态" width="110"><template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag></template></el-table-column><el-table-column label="时间" width="170"><template #default="{ row }">{{ formatDate(row.createdAt) }}</template></el-table-column><el-table-column label="操作" width="280"><template #default="{ row }"><el-button link type="primary" @click="viewRequest(row)">查看</el-button><el-button v-if="isPlatformAdmin && row.status === 'PENDING'" link type="success" @click="openApprove(row)">审批</el-button><el-button v-if="isPlatformAdmin && row.status === 'PENDING'" link type="danger" @click="rejectRequest(row)">驳回</el-button><el-button v-if="isPlatformAdmin && row.status === 'APPROVED'" link type="warning" @click="openDeliver(row)">登记配送</el-button><el-button v-if="isGymAdmin && row.status === 'WAITING_RECEIVE'" link type="success" @click="receiveRequest(row)">确认入库</el-button></template></el-table-column></el-table>
      <el-empty v-if="!loading && !requests.length" description="暂无申请记录" :image-size="56" />
    </div>

    <div v-if="activeTab === 'deliveries' && isPlatformAdmin" class="simple-card table-card" v-loading="loading"><el-table :data="deliveries" stripe><el-table-column prop="id" label="配送单" width="90" /><el-table-column prop="tenantName" label="健身房" min-width="160" /><el-table-column prop="requestId" label="申请单" width="100" /><el-table-column prop="totalQuantity" label="配送数量" width="110" /><el-table-column prop="operatorId" label="操作人" width="100" /><el-table-column label="配送时间" min-width="180"><template #default="{ row }">{{ formatDate(row.deliveredAt) }}</template></el-table-column><el-table-column prop="note" label="备注" min-width="180" /></el-table></div>

    <el-dialog v-model="catalogDialogVisible" :title="editingCatalog ? '编辑器械' : '新增器械'" width="480px"><el-form label-width="80px"><el-form-item label="名称" required><el-input v-model="catalogForm.name" /></el-form-item><el-form-item label="分类" required><el-input v-model="catalogForm.category" placeholder="例如：有氧器械" /></el-form-item><el-form-item label="单位"><el-input v-model="catalogForm.unit" /></el-form-item><el-form-item label="说明"><el-input v-model="catalogForm.description" type="textarea" :rows="3" /></el-form-item><el-form-item v-if="editingCatalog" label="状态"><el-switch v-model="catalogForm.active" active-text="启用" inactive-text="停用" /></el-form-item></el-form><template #footer><el-button @click="catalogDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveCatalog">保存</el-button></template></el-dialog>

    <el-dialog v-model="requestDialogVisible" title="申请器械" width="620px"><el-form label-width="80px"><el-form-item label="申请说明"><el-input v-model="requestNote" type="textarea" :rows="2" /></el-form-item><div v-for="(item, index) in requestItems" :key="index" class="request-line"><el-select v-model="item.catalogId" filterable placeholder="选择器械" style="flex:1"><el-option v-for="catalog in availableCatalogs" :key="catalog.id" :label="`${catalog.name}（${catalog.category}）`" :value="catalog.id" /></el-select><el-input-number v-model="item.quantity" :min="1" :max="10000" /><el-button link type="danger" @click="requestItems.splice(index, 1)">移除</el-button></div><el-button plain @click="requestItems.push({ catalogId: undefined, quantity: 1 })">添加器械</el-button></el-form><template #footer><el-button @click="requestDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submitRequest">提交申请</el-button></template></el-dialog>

    <el-dialog v-model="decisionDialogVisible" :title="decisionMode === 'approve' ? '审批申请' : '登记配送'" width="560px"><el-form label-width="90px"><div v-for="item in selectedRequest?.items ?? []" :key="item.catalogId" class="decision-line"><span>{{ item.name }}（申请 {{ item.requestedQuantity }}，批准 {{ item.approvedQuantity }}）</span><el-input-number v-model="decisionQuantities[item.catalogId]" :min="0" :max="decisionMode === 'approve' ? item.requestedQuantity : item.approvedQuantity" /></div><el-input v-model="decisionComment" type="textarea" :rows="2" placeholder="填写备注" /></el-form><template #footer><el-button @click="decisionDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submitDecision">确认</el-button></template></el-dialog>

    <el-dialog v-model="detailVisible" title="申请详情" width="560px"><template v-if="selectedRequest"><div class="detail-summary"><p><strong>健身房：</strong>{{ selectedRequest.tenantName || `健身房 #${selectedRequest.tenantId}` }}</p><p><strong>状态：</strong><el-tag :type="statusType(selectedRequest.status)">{{ statusLabel(selectedRequest.status) }}</el-tag></p><p><strong>说明：</strong>{{ selectedRequest.note || '—' }}</p></div><el-table :data="selectedRequest.items" size="small"><el-table-column prop="name" label="器械" /><el-table-column prop="requestedQuantity" label="申请" /><el-table-column prop="approvedQuantity" label="批准" /><el-table-column prop="deliveredQuantity" label="配送" /><el-table-column prop="receivedQuantity" label="入库" /></el-table></template></el-dialog>

    <el-dialog v-model="warehouseAdjustmentVisible" title="调整平台仓库库存" width="500px">
      <el-form label-width="90px">
        <el-form-item label="器械"><span>{{ adjustmentItem?.name || '—' }}</span></el-form-item>
        <el-form-item label="调整数量" required><el-input-number v-model="adjustmentDelta" :min="adjustmentItem ? -adjustmentItem.warehouseQuantity : 0" :max="100000" /></el-form-item>
        <el-form-item label="调整原因" required><el-input v-model="adjustmentReason" type="textarea" :rows="3" placeholder="例如：采购入库、盘点盘亏" /></el-form-item>
        <p class="adjustment-hint">调整后仓库可配送：{{ (adjustmentItem?.warehouseQuantity ?? 0) + adjustmentDelta }}</p>
      </el-form>
      <template #footer><el-button @click="warehouseAdjustmentVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submitWarehouseAdjustment">确认调整</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { equipmentApi, type EquipmentCatalog, type EquipmentDelivery, type EquipmentInventory, type EquipmentRequest, type EquipmentRequestStatus, type EquipmentStatistics, type PlatformEquipmentInventory } from '@/api/equipment'
import { tenancyApi } from '@/api/tenancy'
import { useAuthStore } from '@/stores/auth'

type Tab = 'overview' | 'catalog' | 'inventory' | 'requests' | 'deliveries'
type TenantOption = { id: number; name: string; status?: string }
const auth = useAuthStore()
const isPlatformAdmin = computed(() => auth.isPlatformAdmin)
const isGymAdmin = computed(() => auth.roles.includes('GYM_ADMIN'))
const activeTab = ref<Tab>(isPlatformAdmin.value ? 'overview' : 'inventory')
const loading = ref(false)
const saving = ref(false)
const activeOnly = ref(true)
const catalogKeyword = ref('')
const inventoryKeyword = ref('')
const requestStatus = ref<EquipmentRequestStatus>()
const tenants = ref<TenantOption[]>([])
const catalogs = ref<EquipmentCatalog[]>([])
const inventory = ref<EquipmentInventory[]>([])
const requests = ref<EquipmentRequest[]>([])
const deliveries = ref<EquipmentDelivery[]>([])
const statistics = ref<EquipmentStatistics>()
const catalogDialogVisible = ref(false)
const editingCatalog = ref<EquipmentCatalog | null>(null)
const catalogForm = reactive({ name: '', category: '', description: '', unit: '台', active: true })
const requestDialogVisible = ref(false)
const requestNote = ref('')
const requestItems = ref<Array<{ catalogId?: number; quantity: number }>>([])
const decisionDialogVisible = ref(false)
const decisionMode = ref<'approve' | 'deliver'>('approve')
const selectedRequest = ref<EquipmentRequest | null>(null)
const decisionQuantities = reactive<Record<number, number>>({})
const decisionComment = ref('')
const detailVisible = ref(false)
const warehouseAdjustmentVisible = ref(false)
const adjustmentItem = ref<PlatformEquipmentInventory | null>(null)
const adjustmentDelta = ref(0)
const adjustmentReason = ref('')
const statusOptions: Array<{ value: EquipmentRequestStatus; label: string }> = [
  { value: 'PENDING', label: '待审批' }, { value: 'APPROVED', label: '已批准' }, { value: 'REJECTED', label: '已驳回' }, { value: 'DELIVERING', label: '配送中' }, { value: 'WAITING_RECEIVE', label: '待确认' }, { value: 'RECEIVED', label: '已入库' }, { value: 'CANCELLED', label: '已取消' }
]
const filteredCatalogs = computed(() => catalogs.value.filter(item => !catalogKeyword.value.trim() || item.name.toLowerCase().includes(catalogKeyword.value.trim().toLowerCase())))
const availableCatalogs = computed(() => catalogs.value.filter(item => item.active && !requestItems.value.some(line => line.catalogId === item.id)))
const filteredInventory = computed(() => inventory.value.filter(item => { const keyword = inventoryKeyword.value.trim().toLowerCase(); return !keyword || `${item.tenantName || ''} ${item.name}`.toLowerCase().includes(keyword) }))
function statusLabel(status: EquipmentRequestStatus) { return statusOptions.find(item => item.value === status)?.label || status }
function statusType(status: EquipmentRequestStatus) { return status === 'REJECTED' || status === 'CANCELLED' ? 'danger' : status === 'RECEIVED' ? 'success' : status === 'PENDING' ? 'warning' : 'info' }
function formatDate(value?: string) { return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—' }
function requestItemsLabel(request: EquipmentRequest) { return request.items.map(item => `${item.name} × ${item.requestedQuantity}`).join('、') }
function effectiveTenantId() { return isPlatformAdmin.value ? auth.platformContextTenantId ?? tenants.value[0]?.id : undefined }
async function loadTenants() {
  if (!isPlatformAdmin.value) return
  try {
    const response = await tenancyApi.listTenants(0, 200)
    const data = (response as any)?.data
    const rows = Array.isArray(data?.content) ? data.content : Array.isArray(data) ? data : []
    tenants.value = rows
      .map((tenant: Record<string, unknown>) => ({
        id: Number(tenant.id),
        name: String(tenant.name || tenant.code || `健身房 #${tenant.id}`),
        status: String(tenant.status || '')
      }))
      .filter((tenant: TenantOption) => Number.isSafeInteger(tenant.id) && tenant.id > 0 && tenant.status !== 'DISABLED')
      .sort((left: TenantOption, right: TenantOption) => left.id - right.id)
    if ((auth.platformContextTenantId == null || !tenants.value.some(tenant => tenant.id === auth.platformContextTenantId)) && tenants.value[0]) {
      auth.setPlatformContextTenantId(tenants.value[0].id)
    }
  } catch {
    tenants.value = []
  }
}
async function loadCatalog() { try { catalogs.value = (await equipmentApi.listCatalog(activeOnly.value)).data ?? [] } catch { ElMessage.error('器械目录加载失败') } }
async function loadInventory() { try { inventory.value = (await equipmentApi.listInventory(effectiveTenantId())).data ?? [] } catch { ElMessage.error('库存加载失败') } }
async function loadRequests() { try { requests.value = (await equipmentApi.listRequests({ tenantId: effectiveTenantId(), ...(requestStatus.value ? { status: requestStatus.value } : {}) })).data ?? [] } catch { ElMessage.error('申请记录加载失败') } }
async function loadStatistics() { try { statistics.value = (await equipmentApi.statistics(effectiveTenantId())).data } catch { ElMessage.error('统计加载失败') } }
async function loadDeliveries() { try { deliveries.value = (await equipmentApi.listDeliveries(effectiveTenantId())).data ?? [] } catch { ElMessage.error('配送记录加载失败') } }
async function loadActiveTab() { loading.value = true; try { if (activeTab.value === 'overview') await loadStatistics(); if (activeTab.value === 'catalog') await loadCatalog(); if (activeTab.value === 'inventory') await loadInventory(); if (activeTab.value === 'requests') await loadRequests(); if (activeTab.value === 'deliveries') await loadDeliveries() } finally { loading.value = false } }
function openCatalog(catalog?: EquipmentCatalog) { editingCatalog.value = catalog || null; Object.assign(catalogForm, catalog ? { name: catalog.name, category: catalog.category, description: catalog.description || '', unit: catalog.unit, active: catalog.active } : { name: '', category: '', description: '', unit: '台', active: true }); catalogDialogVisible.value = true }
async function saveCatalog() { if (!catalogForm.name.trim() || !catalogForm.category.trim()) { ElMessage.warning('请填写器械名称和分类'); return }; saving.value = true; try { if (editingCatalog.value) await equipmentApi.updateCatalog(editingCatalog.value.id, { ...catalogForm }); else await equipmentApi.createCatalog({ ...catalogForm }); ElMessage.success('器械目录已保存'); catalogDialogVisible.value = false; await loadCatalog() } catch { /* client interceptor提示 */ } finally { saving.value = false } }
function openRequest() { requestNote.value = ''; requestItems.value = [{ catalogId: undefined, quantity: 1 }]; requestDialogVisible.value = true; void loadCatalog() }
async function submitRequest() { const items = requestItems.value.filter(item => item.catalogId && item.quantity > 0).map(item => ({ catalogId: item.catalogId as number, quantity: item.quantity })); if (!items.length) { ElMessage.warning('请至少选择一种器械'); return }; saving.value = true; try { await equipmentApi.createRequest({ note: requestNote.value.trim() || undefined, items }); ElMessage.success('器械申请已提交'); requestDialogVisible.value = false; activeTab.value = 'requests'; await loadRequests() } catch { /* client interceptor提示 */ } finally { saving.value = false } }
function viewRequest(request: EquipmentRequest) { selectedRequest.value = request; detailVisible.value = true }
function openApprove(request: EquipmentRequest) { selectedRequest.value = request; decisionMode.value = 'approve'; decisionComment.value = ''; Object.keys(decisionQuantities).forEach(key => delete decisionQuantities[Number(key)]); request.items.forEach(item => { decisionQuantities[item.catalogId] = item.requestedQuantity }); decisionDialogVisible.value = true }
function openDeliver(request: EquipmentRequest) { selectedRequest.value = request; decisionMode.value = 'deliver'; decisionComment.value = ''; Object.keys(decisionQuantities).forEach(key => delete decisionQuantities[Number(key)]); request.items.forEach(item => { decisionQuantities[item.catalogId] = item.approvedQuantity }); decisionDialogVisible.value = true }
async function submitDecision() { if (!selectedRequest.value) return; saving.value = true; try { if (decisionMode.value === 'approve') await equipmentApi.approve(selectedRequest.value.id, { ...decisionQuantities }, decisionComment.value.trim() || undefined); else await equipmentApi.deliver(selectedRequest.value.id, { ...decisionQuantities }, decisionComment.value.trim() || undefined); ElMessage.success(decisionMode.value === 'approve' ? '申请已审批' : '配送已登记'); decisionDialogVisible.value = false; await loadRequests() } catch { /* client interceptor提示 */ } finally { saving.value = false } }
async function rejectRequest(request: EquipmentRequest) { try { const result = await ElMessageBox.prompt('填写驳回原因（可选）', '驳回申请', { confirmButtonText: '驳回', cancelButtonText: '取消' }); await equipmentApi.reject(request.id, result.value?.trim() || undefined); ElMessage.success('申请已驳回'); await loadRequests() } catch { /* 用户取消 */ } }
async function receiveRequest(request: EquipmentRequest) { saving.value = true; try { await equipmentApi.receive(request.id); ElMessage.success('已确认入库'); await loadRequests(); await loadInventory() } catch { /* client interceptor提示 */ } finally { saving.value = false } }
function openWarehouseAdjustment(item: PlatformEquipmentInventory) { adjustmentItem.value = item; adjustmentDelta.value = 0; adjustmentReason.value = ''; warehouseAdjustmentVisible.value = true }
async function submitWarehouseAdjustment() {
  if (!adjustmentItem.value || adjustmentDelta.value === 0 || !adjustmentReason.value.trim()) { ElMessage.warning('请填写调整数量和原因'); return }
  saving.value = true
  try { await equipmentApi.adjustWarehouse(adjustmentItem.value.catalogId, adjustmentDelta.value, adjustmentReason.value.trim()); await loadStatistics(); ElMessage.success('平台仓库库存已调整'); warehouseAdjustmentVisible.value = false } catch { /* client interceptor提示 */ } finally { saving.value = false }
}
onMounted(async () => { await loadTenants(); await loadCatalog(); await loadActiveTab() })
</script>

<style scoped>
.page-head { display:flex; justify-content:space-between; align-items:flex-start; gap:16px; margin-bottom:12px; }
.page-actions { display:flex; align-items:center; gap:10px; flex-wrap:wrap; justify-content:flex-end; }
.overview-grid { display:grid; grid-template-columns:repeat(6,minmax(120px,1fr)); gap:12px; }
.stat-card { padding:18px; border:1px solid var(--border); border-radius:8px; background:var(--surface); }
.stat-card span { display:block; color:var(--text-muted); font-size:12px; margin-bottom:10px; }
.stat-card strong { font-size:25px; color:var(--primary); }
.table-card { padding:18px; margin-top:14px; }
.overview-table { grid-column:1/-1; }
.section-heading, .toolbar { display:flex; align-items:center; justify-content:space-between; gap:12px; margin-bottom:14px; }
.section-heading h3 { margin:0; font-size:16px; }
.request-line, .decision-line { display:flex; align-items:center; gap:10px; margin-bottom:12px; }
.decision-line span { flex:1; font-size:13px; }
.detail-summary p { margin:0 0 10px; }
.warehouse-stat strong { color: #0f8f64; }
.adjustment-hint { margin: -2px 0 0 90px; color: var(--text-muted); font-size: 13px; }
@media (max-width: 1000px) { .overview-grid { grid-template-columns:repeat(3,minmax(120px,1fr)); } }
@media (max-width: 650px) { .overview-grid { grid-template-columns:repeat(2,minmax(120px,1fr)); } .toolbar { flex-wrap:wrap; } .page-head { flex-direction:column; } .page-actions { width:100%; justify-content:flex-start; } }
</style>
