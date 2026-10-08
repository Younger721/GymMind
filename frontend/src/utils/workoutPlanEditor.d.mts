export const PHASES: readonly ['WARMUP', 'MAIN', 'STRETCH']
export function flattenPlanItems(phaseItems: Record<string, any[]>): any[]
export function planToEditor(plan: any): any
export function validatePlanEditor(form: any, isMember: boolean): string
