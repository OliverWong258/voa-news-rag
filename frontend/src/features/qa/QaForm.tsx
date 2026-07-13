import type { FormEvent } from 'react'
import { CATEGORIES } from '../../config/categories'
import type { QaFormValues } from './qaFormModel'

interface QaFormProps {
  values: QaFormValues
  running: boolean
  onChange: (values: QaFormValues) => void
  onSubmit: () => void
  onStop: () => void
}

export function QaForm({ values, running, onChange, onSubmit, onStop }: QaFormProps) {
  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    onSubmit()
  }

  return (
    <form className="qa-form" onSubmit={handleSubmit}>
      <label className="qa-question-label">
        <span>你的问题</span>
        <textarea
          autoFocus
          disabled={running}
          maxLength={1000}
          placeholder="例如：美国近期对亚洲的外交政策有哪些变化？"
          rows={4}
          value={values.question}
          onChange={(event) => onChange({ ...values, question: event.target.value })}
        />
      </label>

      <div className="qa-filter-row">
        <label>
          <span>类别</span>
          <select
            disabled={running}
            value={values.category}
            onChange={(event) => onChange({ ...values, category: event.target.value })}
          >
            <option value="">全部类别</option>
            {CATEGORIES.map((category) => (
              <option key={category} value={category}>
                {category}
              </option>
            ))}
          </select>
        </label>
        <label>
          <span>开始日期</span>
          <input
            disabled={running}
            type="date"
            value={values.startDate}
            onChange={(event) => onChange({ ...values, startDate: event.target.value })}
          />
        </label>
        <label>
          <span>结束日期</span>
          <input
            disabled={running}
            type="date"
            value={values.endDate}
            onChange={(event) => onChange({ ...values, endDate: event.target.value })}
          />
        </label>
        <label>
          <span>参考片段</span>
          <select
            disabled={running}
            value={values.topK}
            onChange={(event) => onChange({ ...values, topK: Number(event.target.value) })}
          >
            {[5, 8, 10, 20, 30].map((limit) => (
              <option key={limit} value={limit}>
                {limit} 条
              </option>
            ))}
          </select>
        </label>
      </div>

      <div className="qa-actions">
        {running ? (
          <button className="button danger" onClick={onStop} type="button">
            停止生成
          </button>
        ) : (
          <button className="button primary" type="submit">
            开始回答
          </button>
        )}
        <span>{values.question.length}/1000</span>
      </div>
    </form>
  )
}
