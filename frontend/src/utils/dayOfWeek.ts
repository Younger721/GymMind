/** dayOfWeek 与后端一致：1=星期一 … 7=星期日 */
const WEEKDAY_LABELS = ['', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六', '星期日'] as const

export function formatDayOfWeek(day: number): string {
  if (day >= 1 && day <= 7) {
    return WEEKDAY_LABELS[day]
  }
  return day ? String(day) : '—'
}

/** 按相同 dayOfWeek 合并表格首列时的 rowspan（0 表示被上一行合并） */
export function dayOfWeekRowSpans(items: Array<{ dayOfWeek: number }>): number[] {
  const spans = new Array<number>(items.length).fill(1)
  let i = 0
  while (i < items.length) {
    const day = items[i].dayOfWeek
    let count = 1
    while (i + count < items.length && items[i + count].dayOfWeek === day) {
      count += 1
    }
    spans[i] = count
    for (let j = 1; j < count; j += 1) {
      spans[i + j] = 0
    }
    i += count
  }
  return spans
}
