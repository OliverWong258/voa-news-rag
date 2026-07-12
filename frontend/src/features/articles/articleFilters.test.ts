import { describe, expect, it } from 'vitest'
import {
  readArticleFilters,
  validateDateRange,
  writeArticleFilters,
} from './articleFilters'

describe('article filters', () => {
  it('reads supported URL filters and a zero-based page', () => {
    expect(readArticleFilters(new URLSearchParams('category=Asia&startDate=2026-01-01&page=2'))).toEqual({
      category: 'Asia',
      startDate: '2026-01-01',
      endDate: '',
      page: 2,
    })
  })

  it('drops unsupported categories and invalid pages', () => {
    expect(readArticleFilters(new URLSearchParams('category=Unknown&page=-1'))).toMatchObject({
      category: '',
      page: 0,
    })
  })

  it('omits defaults when writing URL filters', () => {
    expect(
      writeArticleFilters({ category: 'Politics', startDate: '', endDate: '', page: 0 }).toString(),
    ).toBe('category=Politics')
  })

  it('rejects a reversed date range', () => {
    expect(
      validateDateRange({ category: '', startDate: '2026-07-12', endDate: '2026-07-01', page: 0 }),
    ).toBe('开始日期不能晚于结束日期')
  })
})

