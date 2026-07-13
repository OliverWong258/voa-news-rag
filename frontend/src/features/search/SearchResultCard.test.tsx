import { describe, expect, it } from 'vitest'
import { highlightedText, relevancePercent } from './searchPresentation'

describe('search result presentation', () => {
  it('clamps cosine similarity into a percentage', () => {
    expect(relevancePercent(0.876)).toBe(88)
    expect(relevancePercent(1.2)).toBe(100)
    expect(relevancePercent(-0.2)).toBe(0)
  })

  it('returns plain text when the query has no highlightable term', () => {
    expect(highlightedText('新闻文本', 'a')).toBe('新闻文本')
  })
})
