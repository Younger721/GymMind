const labels = {
  PENDING: '待审批',
  APPROVED: '已批准',
  REJECTED: '已驳回',
  DELIVERING: '配送中',
  WAITING_RECEIVE: '待确认',
  RECEIVED: '已入库',
  CANCELLED: '已取消'
}

export function equipmentStatusLabel(status) {
  return labels[status] || status || '未知'
}

export function equipmentStatusType(status) {
  if (status === 'REJECTED' || status === 'CANCELLED') return 'danger'
  if (status === 'RECEIVED') return 'success'
  if (status === 'PENDING') return 'warning'
  return 'info'
}

export function summarizeEquipmentInventory(rows = []) {
  return rows.reduce((summary, row) => {
    summary.deliveredQuantity += Number(row.deliveredQuantity || 0)
    summary.receivedQuantity += Number(row.receivedQuantity || 0)
    summary.usableQuantity += Number(row.usableQuantity || 0)
    return summary
  }, { deliveredQuantity: 0, receivedQuantity: 0, usableQuantity: 0 })
}
