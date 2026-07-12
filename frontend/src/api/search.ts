import type { NewsSearchResponse, SearchParams } from './contracts'
import { requestJson } from './http'
import { toQueryString } from './query'

export function searchNews(params: SearchParams, signal?: AbortSignal) {
  return requestJson<NewsSearchResponse>(`/api/search${toQueryString(params)}`, { signal })
}

