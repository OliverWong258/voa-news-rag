import { describe, expect, it } from 'vitest'
import { CATEGORIES } from './categories'

describe('CATEGORIES', () => {
  it('matches the current backend category set', () => {
    expect(CATEGORIES).toEqual(['Africa', 'Asia', 'Politics', 'United States'])
  })
})

