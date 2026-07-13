export interface RawSseEvent {
  event: string
  data: string
}

function parseEventBlock(block: string): RawSseEvent | null {
  let event = 'message'
  const data: string[] = []

  for (const line of block.split(/\r?\n/)) {
    if (!line || line.startsWith(':')) continue
    const separator = line.indexOf(':')
    const field = separator === -1 ? line : line.slice(0, separator)
    let value = separator === -1 ? '' : line.slice(separator + 1)
    if (value.startsWith(' ')) value = value.slice(1)
    if (field === 'event') event = value
    if (field === 'data') data.push(value)
  }

  return data.length === 0 ? null : { event, data: data.join('\n') }
}

export class SseParser {
  private buffer = ''

  push(chunk: string): RawSseEvent[] {
    this.buffer += chunk
    const events: RawSseEvent[] = []
    let match = /\r?\n\r?\n/.exec(this.buffer)

    while (match?.index !== undefined) {
      const block = this.buffer.slice(0, match.index)
      this.buffer = this.buffer.slice(match.index + match[0].length)
      const event = parseEventBlock(block)
      if (event) events.push(event)
      match = /\r?\n\r?\n/.exec(this.buffer)
    }
    return events
  }

  finish(): RawSseEvent[] {
    const block = this.buffer
    this.buffer = ''
    if (!block.trim()) return []
    const event = parseEventBlock(block)
    return event ? [event] : []
  }
}

