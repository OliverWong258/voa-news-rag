import { describe, expect, it } from 'vitest'
import {
  readSearchFilters,
  toSearchParams,
  validateSearchFilters,
  writeSearchFilters,
} from './searchFilters'

describe('search filters', () => {
  it('reads supported search filters from the URL', () => {
    expect(
      readSearchFilters(new URLSearchParams('q=亚洲+外交&category=Asia&topK=20')),
    ).toMatchObject({ q: '亚洲 外交', category: 'Asia', topK: 20 })
  })

  it('normalizes filters for the backend request', () => {
    expect(
      toSearchParams({ q: '  election  ', category: '', startDate: '', endDate: '', topK: 10 }),
    ).toEqual({
      q: 'election',
      category: undefined,
      startDate: undefined,
      endDate: undefined,
      topK: 10,
    })
  })

  it('omits default filters from a shareable URL', () => {
    expect(
      writeSearchFilters({ q: '  Politics ', category: '', startDate: '', endDate: '', topK: 10 }).toString(),
    ).toBe('q=Politics')
  })

  it('validates blank queries and reversed dates', () => {
    expect(validateSearchFilters({ q: '', category: '', startDate: '', endDate: '', topK: 10 })).toBe(
      '请输入要搜索的内容',
    )
    expect(
      validateSearchFilters({
        q: 'news',
        category: '',
        startDate: '2026-07-12',
        endDate: '2026-07-01',
        topK: 10,
      }),
    ).toBe('开始日期不能晚于结束日期')
  })
})

