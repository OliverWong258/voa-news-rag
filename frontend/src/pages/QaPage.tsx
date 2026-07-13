import { useEffect, useRef, useState } from 'react'
import type { RagSource, RagSseEvent } from '../api/contracts'
import { ApiRequestError } from '../api/http'
import { IncompleteSseError, streamQuestion } from '../api/qa'
import { AnswerContent } from '../features/qa/AnswerContent'
import { QaForm } from '../features/qa/QaForm'
import { SourceCard } from '../features/qa/SourceCard'
import {
  DEFAULT_QA_FORM,
  toRagQuestionRequest,
  validateQaForm,
  type QaFormValues,
} from '../features/qa/qaFormModel'

type CompletionState = 'idle' | 'running' | 'grounded' | 'ungrounded' | 'stopped' | 'error'

export function QaPage() {
  const [form, setForm] = useState<QaFormValues>(DEFAULT_QA_FORM)
  const [answer, setAnswer] = useState('')
  const [sources, setSources] = useState<RagSource[]>([])
  const [status, setStatus] = useState<CompletionState>('idle')
  const [error, setError] = useState<string | null>(null)
  const abortController = useRef<AbortController | null>(null)
  const running = status === 'running'

  useEffect(() => () => abortController.current?.abort(), [])

  function handleEvent(event: RagSseEvent) {
    if (event.event === 'sources') setSources(event.data.sources)
    if (event.event === 'token') setAnswer((current) => current + event.data.text)
    if (event.event === 'completed') setStatus(event.data.grounded ? 'grounded' : 'ungrounded')
  }

  async function submit() {
    const validationError = validateQaForm(form)
    if (validationError) {
      setError(validationError)
      return
    }

    abortController.current?.abort()
    const controller = new AbortController()
    abortController.current = controller
    setAnswer('')
    setSources([])
    setError(null)
    setStatus('running')

    try {
      await streamQuestion(toRagQuestionRequest(form), handleEvent, controller.signal)
    } catch (caught) {
      if (controller.signal.aborted) {
        setStatus('stopped')
        return
      }
      setStatus('error')
      if (caught instanceof ApiRequestError || caught instanceof IncompleteSseError) setError(caught.message)
      else setError('智能回答失败，请稍后重试。')
    } finally {
      if (abortController.current === controller) abortController.current = null
    }
  }

  function stop() {
    abortController.current?.abort()
  }

  return (
    <section className="qa-page">
      <header className="qa-heading">
        <p className="eyebrow">Grounded answers</p>
        <h1>智能问答</h1>
        <p>根据已翻译的 VOA 新闻回答问题。答案中的编号可以直接定位到支持该回答的新闻片段。</p>
      </header>

      <QaForm values={form} running={running} onChange={setForm} onStop={stop} onSubmit={() => void submit()} />

      {error && (
        <div className="status-panel error qa-error" role="alert">
          <div>
            <strong>{status === 'error' ? '回答生成失败' : '请检查问题'}</strong>
            <p>{error}</p>
          </div>
          {status === 'error' && (
            <button className="button secondary" onClick={() => void submit()} type="button">
              重新生成
            </button>
          )}
        </div>
      )}

      {status !== 'idle' && (
        <>
          <AnswerContent answer={answer} running={running} sources={sources} />

          {status === 'grounded' && (
            <div className="grounding-status grounded">回答已由下列新闻来源支持</div>
          )}
          {status === 'ungrounded' && (
            <div className="grounding-status ungrounded">当前新闻库中没有足够证据支持回答</div>
          )}
          {status === 'stopped' && <div className="grounding-status stopped">生成已停止，以上是已接收的内容</div>}

          {sources.length > 0 && (
            <section className="sources-section">
              <div className="sources-heading">
                <h2>引用来源</h2>
                <span>{sources.length} 个相关片段</span>
              </div>
              <div className="sources-list">
                {sources.map((source) => (
                  <SourceCard key={`${source.citation}-${source.chunkId}`} source={source} />
                ))}
              </div>
            </section>
          )}
        </>
      )}
    </section>
  )
}
