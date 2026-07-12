import type { ArticleListParams } from '../../api/contracts'
import { CATEGORIES } from '../../config/categories'

export const ARTICLE_PAGE_SIZE = 12

export interface ArticleFilters {
  category: string
  startDate: string
  endDate: string
  page: number
}

export const DEFAULT_ARTICLE_FILTERS: ArticleFilters = {
  category: '',
  startDate: '',
  endDate: '',
  page: 0,
}

export function readArticleFilters(searchParams: URLSearchParams): ArticleFilters {
  const category = searchParams.get('category') ?? ''
  const startDate = searchParams.get('startDate') ?? ''
  const endDate = searchParams.get('endDate') ?? ''
  const pageValue = Number(searchParams.get('page'))

  return {
    category: CATEGORIES.some((item) => item === category) ? category : '',
    startDate,
    endDate,
    page: Number.isInteger(pageValue) && pageValue >= 0 ? pageValue : 0,
  }
}

export function writeArticleFilters(filters: ArticleFilters): URLSearchParams {
  const params = new URLSearchParams()
  if (filters.category) params.set('category', filters.category)
  if (filters.startDate) params.set('startDate', filters.startDate)
  if (filters.endDate) params.set('endDate', filters.endDate)
  if (filters.page > 0) params.set('page', String(filters.page))
  return params
}

export function toArticleListParams(filters: ArticleFilters): ArticleListParams {
  return {
    category: filters.category || undefined,
    startDate: filters.startDate || undefined,
    endDate: filters.endDate || undefined,
    page: filters.page,
    size: ARTICLE_PAGE_SIZE,
  }
}

export function validateDateRange(filters: ArticleFilters): string | null {
  if (filters.startDate && filters.endDate && filters.startDate > filters.endDate) {
    return '开始日期不能晚于结束日期'
  }
  return null
}

