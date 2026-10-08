export const STANDARD_CATEGORY_LABELS = {
  CHEST: '胸',
  BACK: '背',
  LEGS: '腿',
  SHOULDERS: '肩',
  ARMS: '手臂',
  CORE: '核心',
  CARDIO: '有氧'
}

export function exerciseCategoryLabel(category) {
  return STANDARD_CATEGORY_LABELS[category] || category || ''
}

export function exerciseCategories(exercises) {
  return [...new Set(exercises.map(item => item.category).filter(Boolean))]
    .sort((a, b) => exerciseCategoryLabel(a).localeCompare(exerciseCategoryLabel(b), 'zh-CN'))
}

export function filterExercises(exercises, { keyword = '', category = '', activeOnly = false } = {}) {
  const normalizedKeyword = keyword.trim().toLowerCase()
  return exercises.filter(item => {
    const searchable = [item.name, item.category, exerciseCategoryLabel(item.category), item.targetMuscle, item.equipment, item.tags]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
    return (!activeOnly || item.active) && (!category || item.category === category)
      && (!normalizedKeyword || searchable.includes(normalizedKeyword))
  })
}
