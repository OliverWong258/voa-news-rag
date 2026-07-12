import { describe, expect, it } from 'vitest'
import { safeReturnPath, splitArticleContent } from './articlePresentation'

describe('article presentation', () => {
  it('splits translated content into paragraphs across line ending styles', () => {
    expect(splitArticleContent('第一段。\r\n\r\n第二段。\n\n 第三段。 ')).toEqual([
      '第一段。',
      '第二段。',
      '第三段。',
    ])
  })

  it('keeps a local list return path with its filters', () => {
    expect(safeReturnPath('/?category=Asia&page=2')).toBe('/?category=Asia&page=2')
  })

  it('rejects external and protocol-relative return paths', () => {
    expect(safeReturnPath('https://example.com')).toBe('/')
    expect(safeReturnPath('//example.com')).toBe('/')
  })
})

