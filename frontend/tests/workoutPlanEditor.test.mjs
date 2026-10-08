import test from 'node:test'
import assert from 'node:assert/strict'
import { flattenPlanItems, planToEditor, validatePlanEditor } from '../src/utils/workoutPlanEditor.mjs'

test('flattens text actions by phase and regenerates phase-local order', () => {
  const items = flattenPlanItems({
    WARMUP: [{ rowId: 4, exerciseName: '开合跳', sets: 2, reps: 20, restSeconds: 15, weight: 0, notes: '' }],
    MAIN: [
      { rowId: 8, exerciseName: '卧推', sets: 4, reps: 10, restSeconds: 90, weight: 20, notes: '控制下放' },
      { rowId: 9, exerciseName: '飞鸟', sets: 3, reps: 12, restSeconds: 60, weight: 10, notes: '' }
    ],
    STRETCH: []
  })

  assert.deepEqual(items.map(item => [item.exerciseName, item.phase, item.sortOrder]), [
    ['开合跳', 'WARMUP', 1],
    ['卧推', 'MAIN', 1],
    ['飞鸟', 'MAIN', 2]
  ])
  assert.equal(items[1].exerciseId, undefined)
})

test('loads a saved plan into editable phase rows', () => {
  const form = planToEditor({
    id: 12,
    name: '上肢',
    goal: '力量',
    description: '训练说明',
    startDate: '2026-10-08',
    endDate: '2026-11-08',
    items: [{ id: 3, exerciseName: '卧推', phase: 'MAIN', sortOrder: 1, sets: 4, reps: 8, restSeconds: 90, weight: 30, notes: '' }]
  })

  assert.equal(form.id, 12)
  assert.equal(form.phaseItems.MAIN[0].exerciseName, '卧推')
  assert.equal(form.phaseItems.WARMUP.length, 0)
})

test('rejects missing text action and invalid training values with row context', () => {
  const message = validatePlanEditor({
    name: '计划',
    goal: '力量',
    category: '胸',
    phaseItems: { WARMUP: [], MAIN: [{ rowId: 2, exerciseName: ' ', sets: 0, reps: 10, restSeconds: -1, weight: 0, notes: '' }], STRETCH: [] }
  }, false)

  assert.equal(message, '正式组第1个动作：请填写动作名称')
})
