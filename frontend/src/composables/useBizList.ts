import { ref } from 'vue'
import { ElMessage } from 'element-plus'

export function useBizList<T extends Record<string, unknown>>() {
  const rows = ref<T[]>([])
  const loading = ref(false)
  const result = ref<unknown>(null)

  async function runLoader(loader: () => Promise<{ data?: T[] | { content?: T[] } } | T[]>) {
    loading.value = true
    result.value = null
    try {
      const res = await loader()
      if (Array.isArray(res)) {
        rows.value = res as T[]
      } else if (Array.isArray((res as { data?: T[] }).data)) {
        rows.value = (res as { data: T[] }).data
      } else if ((res as { data?: { content?: T[] } }).data?.content) {
        rows.value = (res as { data: { content: T[] } }).data.content
      } else {
        rows.value = []
        result.value = res
      }
    } catch {
      rows.value = []
      ElMessage.error('加载失败')
    } finally {
      loading.value = false
    }
  }

  return { rows, loading, result, runLoader }
}
