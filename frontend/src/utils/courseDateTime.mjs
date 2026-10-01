const LOCAL_DATE_TIME = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})$/

export function toUtcIso(value) {
  const match = LOCAL_DATE_TIME.exec(String(value ?? ''))
  if (!match) throw new Error('Invalid local date-time')
  const date = new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]), Number(match[4]), Number(match[5]), Number(match[6]), 0)
  if (Number.isNaN(date.getTime())) throw new Error('Invalid local date-time')
  return date.toISOString()
}

export function toLocalDateTime(value) {
  const date = new Date(String(value ?? ''))
  if (Number.isNaN(date.getTime())) throw new Error('Invalid UTC date-time')
  const pad = number => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}
