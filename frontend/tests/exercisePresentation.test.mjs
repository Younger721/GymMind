import test from 'node:test'
import assert from 'node:assert/strict'
import { exerciseCategories, filterExercises, exerciseCategoryLabel } from '../src/utils/exercisePresentation.mjs'

const exercises = [
  { id: 1, name: '卧推', category: 'CHEST', targetMuscle: '胸大肌', equipment: '杠铃', tags: '推胸,力量', active: true },
  { id: 2, name: '绳索飞鸟', category: '胸部塑形', targetMuscle: '胸肌', equipment: '龙门架', tags: '夹胸', active: true },
  { id: 3, name: '深蹲', category: '臀腿', targetMuscle: '臀大肌', equipment: '杠铃', tags: '下肢', active: true }
]

test('derives only used categories and translates standard labels', () => {
  assert.deepEqual([...exerciseCategories(exercises)].sort(), ['CHEST', '胸部塑形', '臀腿'])
  assert.equal(exerciseCategoryLabel('CHEST'), '胸')
  assert.equal(exerciseCategoryLabel('臀腿'), '臀腿')
})

test('filters by exact category and keyword across exercise metadata', () => {
  assert.deepEqual(filterExercises(exercises, { category: '臀腿' }).map(item => item.name), ['深蹲'])
  assert.deepEqual(filterExercises(exercises, { keyword: '龙门' }).map(item => item.name), ['绳索飞鸟'])
  assert.deepEqual(filterExercises(exercises, { keyword: '推胸' }).map(item => item.name), ['卧推'])
  assert.deepEqual(filterExercises(exercises, { keyword: 'CHEST' }).map(item => item.name), ['卧推'])
})

test('matches Chinese standard category labels and trims case-insensitive queries', () => {
  const rows = [{ name: 'Bench press', category: 'CHEST', active: true }]
  assert.deepEqual(filterExercises(rows, { keyword: '胸' }), rows)
  assert.deepEqual(filterExercises(rows, { keyword: '  BENCH  ' }), rows)
})

test('combines category, keyword and active state without changing the source', () => {
  const rows = [...exercises, { id: 4, name: '臀桥', category: '臀腿', active: false }]
  assert.deepEqual(filterExercises(rows, { category: '臀腿', keyword: '臀', activeOnly: true }).map(row => row.id), [3])
  assert.equal(rows.length, 4)
  assert.equal(exerciseCategories([...exercises, exercises[0]]).length, 3)
  assert.deepEqual(exerciseCategories([]), [])
})
