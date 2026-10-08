export const PHASES = ['WARMUP', 'MAIN', 'STRETCH']

export function flattenPlanItems(phaseItems) {
  return PHASES.flatMap(phase => (phaseItems[phase] || []).map((item, index) => ({
    exerciseName: item.exerciseName?.trim() || '',
    exerciseId: item.exerciseId || undefined,
    dayOfWeek: item.dayOfWeek || 1,
    phase,
    sortOrder: index + 1,
    sets: item.sets,
    reps: item.reps,
    restSeconds: item.restSeconds,
    weight: item.weight || 0,
    notes: item.notes?.trim() || ''
  })))
}

export function planToEditor(plan) {
  const phaseItems = { WARMUP: [], MAIN: [], STRETCH: [] }
  for (const item of plan.items || []) {
    const phase = PHASES.includes(item.phase) ? item.phase : 'MAIN'
    phaseItems[phase].push({
      rowId: item.id || `${phase}-${item.sortOrder}`,
      exerciseId: item.exerciseId || null,
      exerciseName: item.exerciseName || '',
      sortOrder: item.sortOrder || 0,
      dayOfWeek: item.dayOfWeek || 1,
      sets: item.sets,
      reps: item.reps,
      restSeconds: item.restSeconds,
      weight: item.weight || 0,
      notes: item.notes || ''
    })
  }
  for (const phase of PHASES) phaseItems[phase].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
  return { id: plan.id, name: plan.name || '', goal: plan.goal || '', category: plan.category || '', description: plan.description || '', startDate: plan.startDate || '', endDate: plan.endDate || '', phaseItems }
}

export function validatePlanEditor(form, isMember) {
  if (!form.name?.trim() || !form.goal?.trim() || (!isMember && !form.category?.trim())) return '请填写计划名称、目标和分类'
  const items = flattenPlanItems(form.phaseItems)
  if (!items.length) return '请至少添加一个训练动作'
  for (const phase of PHASES) {
    const rows = form.phaseItems[phase] || []
    for (let index = 0; index < rows.length; index += 1) {
      const item = rows[index]
      const label = `${phase === 'WARMUP' ? '热身' : phase === 'STRETCH' ? '拉伸' : '正式组'}第${index + 1}个动作`
      if (!item.exerciseName?.trim()) return `${label}：请填写动作名称`
      if (!Number.isInteger(item.reps) || item.reps < 1) return `${label}：次数必须至少为1`
      if (!Number.isInteger(item.sets) || item.sets < 1) return `${label}：组数必须至少为1`
      if (!Number.isInteger(item.restSeconds) || item.restSeconds < 0) return `${label}：休息秒数不能小于0`
    }
  }
  return ''
}
