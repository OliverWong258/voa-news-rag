import type { RagAnswerResponse, RagQuestionRequest } from './contracts'
import { requestJson } from './http'

export function askQuestion(request: RagQuestionRequest, signal?: AbortSignal) {
  return requestJson<RagAnswerResponse>('/api/qa', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
    signal,
  })
}

