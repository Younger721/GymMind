import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export type EquipmentRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'DELIVERING' | 'WAITING_RECEIVE' | 'RECEIVED' | 'CANCELLED'

export interface EquipmentCatalog {
  id: number
  name: string
  category: string
  description?: string
  unit: string
  active: boolean
}

export interface EquipmentInventory {
  tenantId: number
  tenantName?: string
  catalogId: number
  name: string
  category: string
  unit: string
  deliveredQuantity: number
  receivedQuantity: number
  usableQuantity: number
  abnormalQuantity: number
  lastReceivedAt?: string
}

export interface EquipmentRequestItem {
  catalogId: number
  name: string
  requestedQuantity: number
  approvedQuantity: number
  deliveredQuantity: number
  receivedQuantity: number
}

export interface EquipmentRequest {
  id: number
  tenantId: number
  tenantName?: string
  requesterId: number
  status: EquipmentRequestStatus
  note?: string
  reviewerId?: number
  reviewedAt?: string
  reviewComment?: string
  items: EquipmentRequestItem[]
  createdAt?: string
}

export interface EquipmentDelivery {
  id: number
  tenantId: number
  tenantName?: string
  requestId: number
  operatorId: number
  totalQuantity: number
  deliveredAt: string
  note?: string
}

export interface EquipmentStatistics {
  tenantCount: number
  catalogCount: number
  deliveredQuantity: number
  receivedQuantity: number
  pendingRequests: number
  waitingReceive: number
  inventory: Array<{ tenantId: number; tenantName?: string; catalogName: string; quantity: number }>
}

export const equipmentApi = {
  listCatalog(activeOnly = false) {
    return apiClient.get<any, ApiResponse<EquipmentCatalog[]>>('/v1/equipment/catalog', { params: { activeOnly } })
  },
  createCatalog(body: Pick<EquipmentCatalog, 'name' | 'category' | 'description' | 'unit'>) {
    return apiClient.post<any, ApiResponse<EquipmentCatalog>>('/v1/equipment/catalog', body)
  },
  updateCatalog(id: number, body: Pick<EquipmentCatalog, 'name' | 'category' | 'description' | 'unit'> & { active: boolean }) {
    return apiClient.patch<any, ApiResponse<EquipmentCatalog>>(`/v1/equipment/catalog/${id}`, body)
  },
  listInventory(tenantId?: number) {
    return apiClient.get<any, ApiResponse<EquipmentInventory[]>>('/v1/equipment/inventory', { params: tenantId ? { tenantId } : undefined })
  },
  listRequests(params?: { tenantId?: number; status?: EquipmentRequestStatus }) {
    return apiClient.get<any, ApiResponse<EquipmentRequest[]>>('/v1/equipment/requests', { params })
  },
  createRequest(body: { note?: string; items: Array<{ catalogId: number; quantity: number }> }) {
    return apiClient.post<any, ApiResponse<EquipmentRequest>>('/v1/equipment/requests', body)
  },
  approve(id: number, quantities: Record<number, number>, comment?: string) {
    return apiClient.post<any, ApiResponse<EquipmentRequest>>(`/v1/equipment/requests/${id}/approve`, { quantities, comment })
  },
  reject(id: number, comment?: string) {
    return apiClient.post<any, ApiResponse<EquipmentRequest>>(`/v1/equipment/requests/${id}/reject`, { comment })
  },
  deliver(id: number, quantities: Record<number, number>, comment?: string) {
    return apiClient.post<any, ApiResponse<EquipmentRequest>>(`/v1/equipment/requests/${id}/deliver`, { quantities, comment })
  },
  receive(id: number) {
    return apiClient.post<any, ApiResponse<EquipmentRequest>>(`/v1/equipment/requests/${id}/receive`)
  },
  listDeliveries(tenantId?: number) {
    return apiClient.get<any, ApiResponse<EquipmentDelivery[]>>('/v1/equipment/deliveries', { params: tenantId ? { tenantId } : undefined })
  },
  statistics(tenantId?: number) {
    return apiClient.get<any, ApiResponse<EquipmentStatistics>>('/v1/equipment/statistics', { params: tenantId ? { tenantId } : undefined })
  }
}
