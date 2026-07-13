import type { ReactNode } from 'react'
import type { RagSource } from '../../api/contracts'

export function renderAnswerWithCitations(answer: string, sources: RagSource[]): ReactNode[] {
  const citations = new Map(sources.map((source) => [source.citation, source]))
  return answer.split(/(\[\d+\])/g).map((part, index) => {
    const match = /^\[(\d+)\]$/.exec(part)
    if (!match) return part
    const citation = Number(match[1])
    if (!citations.has(citation)) return part
    return (
      <a
        aria-label={`查看引用 ${citation}`}
        className="inline-citation"
        href={`#source-${citation}`}
        key={`${index}-${citation}`}
      >
        [{citation}]
      </a>
    )
  })
}

