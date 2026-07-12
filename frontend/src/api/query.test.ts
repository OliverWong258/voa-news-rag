import { describe, expect, it } from 'vitest'
import { toQueryString } from './query'

describe('toQueryString', () => {
  it('serializes defined filters and omits empty values', () => {
    expect(
      toQueryString({
        category: 'United States',
        startDate: '2026-07-01',
        endDate: undefined,
        page: 0,
      }),
    ).toBe('?category=United+States&startDate=2026-07-01&page=0')
  })
})

