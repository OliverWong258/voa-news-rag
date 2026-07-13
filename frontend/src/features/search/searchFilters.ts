import type { SearchParams } from '../../api/contracts'
import { CATEGORIES } from '../../config/categories'

export const SEARCH_RESULT_LIMITS = [5, 10, 20, 30] as const

export interface SearchFilters {
  q: string
  category: string
  startDate: string
  endDate: string
  topK: number
}

export const DEFAULT_SEARCH_FILTERS: SearchFilters = {
  q: '',
  category: '',
  startDate: '',
  endDate: '',
  topK: 10,
}

export function readSearchFilters(searchParams: URLSearchParams): SearchFilters {
  const category = searchParams.get('category') ?? ''
  const topKValue = Number(searchParams.get('topK'))

  return {
    q: (searchParams.get('q') ?? '').trim(),
    category: CATEGORIES.some((item) => item === category) ? category : '',
    startDate: searchParams.get('startDate') ?? '',
    endDate: searchParams.get('endDate') ?? '',
    topK: SEARCH_RESULT_LIMITS.some((item) => item === topKValue) ? topKValue : 10,
  }
}

export function writeSearchFilters(filters: SearchFilters): URLSearchParams {
  const params = new URLSearchParams()
  const query = filters.q.trim()
  if (query) params.set('q', query)
  if (filters.category) params.set('category', filters.category)
  if (filters.startDate) params.set('startDate', filters.startDate)
  if (filters.endDate) params.set('endDate', filters.endDate)
  if (filters.topK !== DEFAULT_SEARCH_FILTERS.topK) params.set('topK', String(filters.topK))
  return params
}

export function toSearchParams(filters: SearchFilters): SearchParams {
  return {
    q: filters.q.trim(),
    category: filters.category || undefined,
    startDate: filters.startDate || undefined,
    endDate: filters.endDate || undefined,
    topK: filters.topK,
  }
}

export function validateSearchFilters(filters: SearchFilters): string | null {
  if (!filters.q.trim()) return '请输入要搜索的内容'
  if (filters.q.trim().length > 500) return '搜索内容不能超过 500 个字符'
  if (filters.startDate && filters.endDate && filters.startDate > filters.endDate) {
    return '开始日期不能晚于结束日期'
  }
  return null
}

