import type { RagSource } from '../../api/contracts'
import { renderAnswerWithCitations } from './answerPresentation'

interface AnswerContentProps {
  answer: string
  sources: RagSource[]
  running: boolean
}

export function AnswerContent({ answer, sources, running }: AnswerContentProps) {
  return (
    <section aria-live="polite" className="answer-panel">
      <div className="answer-label">
        <span>{running ? '正在生成回答' : '回答'}</span>
        {running && <span aria-hidden="true" className="streaming-dot" />}
      </div>
      <div className="answer-text">
        {answer ? renderAnswerWithCitations(answer, sources) : <span className="answer-waiting">正在检索相关新闻…</span>}
        {running && answer && <span aria-hidden="true" className="answer-cursor" />}
      </div>
    </section>
  )
}
