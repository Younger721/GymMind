export interface ExerciseSearchItem {
  name?: string
  category?: string
  targetMuscle?: string
  equipment?: string
  tags?: string
  active?: boolean
}

export const STANDARD_CATEGORY_LABELS: Record<string, string>
export function exerciseCategoryLabel(category?: string): string
export function exerciseCategories(exercises: ExerciseSearchItem[]): string[]
export function filterExercises<T extends ExerciseSearchItem>(exercises: T[], options?: { keyword?: string; category?: string; activeOnly?: boolean }): T[]
