import type { ReactNode } from 'react'

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

export function highlightedText(text: string, query: string): ReactNode {
  const terms = query
    .trim()
    .split(/\s+/)
    .filter((term) => term.length >= 2)
    .slice(0, 8)
  if (terms.length === 0) return text

  const pattern = new RegExp(`(${terms.map(escapeRegExp).join('|')})`, 'gi')
  return text
    .split(pattern)
    .map((part, index) => (index % 2 === 1 ? <mark key={`${index}-${part}`}>{part}</mark> : part))
}

export function relevancePercent(score: number): number {
  return Math.round(Math.min(1, Math.max(0, score)) * 100)
}

