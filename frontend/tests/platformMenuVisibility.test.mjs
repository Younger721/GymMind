import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const menuSource = fs.readFileSync(path.join(root, 'src/utils/menuPermissions.ts'), 'utf8')
const routerSource = fs.readFileSync(path.join(root, 'src/router/index.ts'), 'utf8')
const assistantSource = fs.readFileSync(path.join(root, 'src/views/AssistantView.vue'), 'utf8')

test('platform console is hidden from the sidebar and direct access redirects home', () => {
  const visibleCodes = menuSource.match(/const VISIBLE_MENU_CODES[\s\S]*?\n\]\)/)?.[0] ?? ''
  assert.doesNotMatch(visibleCodes, /MENU_CODES\.PLATFORM/)
  assert.match(routerSource, /path: 'platform', name: 'platform', redirect: '\/dashboard'/)
  assert.doesNotMatch(assistantSource, /'\/platform'/)
})
