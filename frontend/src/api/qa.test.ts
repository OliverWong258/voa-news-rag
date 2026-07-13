import { afterEach, describe, expect, it, vi } from 'vitest'
import type { RagSseEvent } from './contracts'
import { IncompleteSseError, streamQuestion } from './qa'

function sseResponse(chunks: Uint8Array[]): Response {
  return new Response(
    new ReadableStream({
      start(controller) {
        chunks.forEach((chunk) => controller.enqueue(chunk))
        controller.close()
      },
    }),
    { headers: { 'Content-Type': 'text/event-stream;charset=UTF-8' } },
  )
}

afterEach(() => vi.unstubAllGlobals())

describe('streamQuestion', () => {
  it('decodes UTF-8 split across chunks and dispatches events in order', async () => {
    const bytes = new TextEncoder().encode(
      'event:sources\ndata:{"sources":[]}\n\nevent:token\ndata:{"text":"你好"}\n\nevent:completed\ndata:{"grounded":true}\n\n',
    )
    const splitAt = bytes.indexOf(0xe5) + 1
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(sseResponse([bytes.slice(0, splitAt), bytes.slice(splitAt)])))
    const events: RagSseEvent[] = []

    await streamQuestion({ question: '测试' }, (event) => events.push(event))

    expect(events.map((event) => event.event)).toEqual(['sources', 'token', 'completed'])
    expect(events[1]).toEqual({ event: 'token', data: { text: '你好' } })
  })

  it('rejects a stream that closes without completed', async () => {
    const bytes = new TextEncoder().encode('event:token\ndata:{"text":"部分回答"}\n\n')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(sseResponse([bytes])))

    await expect(streamQuestion({ question: '测试' }, () => undefined)).rejects.toBeInstanceOf(
      IncompleteSseError,
    )
  })
})

