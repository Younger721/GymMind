export interface SseEvent {
  type: string
  data: string
}

type SseReader = Pick<ReadableStreamDefaultReader<Uint8Array>, 'read' | 'cancel'>

/**
 * Consume an SSE response without waiting for a second read after `done`.
 * Some servlet clients close the socket immediately after the final event,
 * which can otherwise surface as a `terminated` read error in the browser.
 */
export async function consumeSseStream(reader: SseReader, onEvent: (event: SseEvent) => void) {
  const decoder = new TextDecoder()
  let buffer = ''
  let finished = false
  let readerClosed = false

  const consume = (event: string) => {
    const lines = event.split(/\r?\n/)
    const type = lines.find(line => line.startsWith('event:'))?.slice(6).trim() || ''
    const data = lines
      .filter(line => line.startsWith('data:'))
      .map(line => line.slice(5).trimStart())
      .join('\n')
    if (!data) return
    if (type === 'done') finished = true
    onEvent({ type, data })
  }

  try {
    while (!finished) {
      const { value, done } = await reader.read()
      if (done) {
        readerClosed = true
        buffer += decoder.decode()
        if (buffer.trim()) consume(buffer)
        break
      }
      buffer += decoder.decode(value, { stream: true })
      const events = buffer.split(/\r?\n\r?\n/)
      buffer = events.pop() || ''
      for (const event of events) {
        consume(event)
        if (finished) break
      }
    }
  } finally {
    if (!readerClosed) {
      await reader.cancel().catch(() => undefined)
    }
  }
}
