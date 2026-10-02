export function equipmentStatusLabel(status?: string): string
export function equipmentStatusType(status?: string): 'danger' | 'success' | 'warning' | 'info'
export function summarizeEquipmentInventory(rows?: Array<{ deliveredQuantity?: number; receivedQuantity?: number; usableQuantity?: number }>): { deliveredQuantity: number; receivedQuantity: number; usableQuantity: number }
