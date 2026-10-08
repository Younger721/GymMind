import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const readSource = relativePath => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('AI configuration uses a dedicated menu available to gym admins', () => {
  const menus = readSource('src/utils/menuPermissions.ts')
  const router = readSource('src/router/index.ts')
  const rolePermissions = readSource('src/views/business/RolePermissionView.vue')
  const assistant = readSource('src/views/AssistantView.vue')
  const dashboard = readSource('src/views/DashboardView.vue')

  assert.match(menus, /AI_CONFIG:\s*'menu:ai-config'/)
  assert.match(menus, /label:\s*'AI 配置',\s*group:\s*'智能',\s*path:\s*'\/agents\/manage'/)
  assert.match(menus, /\['agent:',\s*MENU_CODES\.AI_CONFIG\]/)
  assert.match(router, /path:\s*'agents\/manage'.*menuPermission:\s*MENU_CODES\.AI_CONFIG/)
  assert.match(assistant, /const configLink = '\/agents\/manage'/)
  assert.match(assistant, /hasPermission\(MENU_CODES\.AI_CONFIG\)/)
  assert.match(dashboard, /path:\s*'\/agents\/manage',\s*permission:\s*MENU_CODES\.AI_CONFIG/)
  assert.match(rolePermissions, /GYM_ADMIN:[\s\S]*?'menu:ai-config'/)
  assert.doesNotMatch(rolePermissions.match(/COACH:[\s\S]*?MEMBER:/)?.[0] ?? '', /menu:ai-config/)
})
