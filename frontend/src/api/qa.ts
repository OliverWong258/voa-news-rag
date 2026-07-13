import type { RagAnswerResponse, RagQuestionRequest, RagSource, RagSseEvent } from './contracts'
import { apiErrorFromResponse, ApiRequestError, requestJson } from './http'
import { SseParser, type RawSseEvent } from './sse'

export function askQuestion(request: RagQuestionRequest, signal?: AbortSignal) {
  return requestJson<RagAnswerResponse>('/api/qa', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
    signal,
  })
}

export class IncompleteSseError extends Error {
  constructor() {
    super('回答流在完成前意外中断')
    this.name = 'IncompleteSseError'
  }
}

function toRagEvent(raw: RawSseEvent): RagSseEvent | null {
  if (raw.event !== 'sources' && raw.event !== 'token' && raw.event !== 'completed') return null
  let data: unknown
  try {
    data = JSON.parse(raw.data)
  } catch {
    throw new ApiRequestError('回答流包含无法解析的数据', 502, 'INVALID_SSE_DATA')
  }

  if (!data || typeof data !== 'object') {
    throw new ApiRequestError('回答流事件格式无效', 502, 'INVALID_SSE_EVENT')
  }
  if (raw.event === 'sources' && Array.isArray((data as { sources?: unknown }).sources)) {
    return { event: 'sources', data: data as { sources: RagSource[] } }
  }
  if (raw.event === 'token' && typeof (data as { text?: unknown }).text === 'string') {
    return { event: 'token', data: data as { text: string } }
  }
  if (raw.event === 'completed' && typeof (data as { grounded?: unknown }).grounded === 'boolean') {
    return { event: 'completed', data: data as { grounded: boolean } }
  }
  throw new ApiRequestError('回答流事件缺少必要字段', 502, 'INVALID_SSE_EVENT')
}

export async function streamQuestion(
  request: RagQuestionRequest,
  onEvent: (event: RagSseEvent) => void,
  signal?: AbortSignal,
): Promise<void> {
  const response = await fetch('/api/qa/stream', {
    method: 'POST',
    headers: {
      Accept: 'text/event-stream',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
    signal,
  })

  if (!response.ok) throw await apiErrorFromResponse(response)
  if (!response.body) throw new ApiRequestError('浏览器没有收到回答数据流', 502, 'MISSING_SSE_BODY')

  const contentType = response.headers.get('content-type') ?? ''
  if (!contentType.toLowerCase().includes('text/event-stream')) {
    throw new ApiRequestError('服务器返回了非 SSE 响应', 502, 'INVALID_SSE_CONTENT_TYPE')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  const parser = new SseParser()
  let completed = false

  const dispatch = (events: RawSseEvent[]) => {
    for (const rawEvent of events) {
      const event = toRagEvent(rawEvent)
      if (!event) continue
      if (event.event === 'completed') completed = true
      onEvent(event)
    }
  }

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      dispatch(parser.push(decoder.decode(value, { stream: true })))
    }
    dispatch(parser.push(decoder.decode()))
    dispatch(parser.finish())
  } finally {
    reader.releaseLock()
  }

  if (!completed) throw new IncompleteSseError()
}
