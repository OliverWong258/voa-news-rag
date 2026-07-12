import type { FormEvent } from 'react'
import { useState } from 'react'
import { CATEGORIES } from '../../config/categories'
import type { ArticleFilters } from './articleFilters'

interface ArticleFiltersFormProps {
  filters: ArticleFilters
  onApply: (filters: ArticleFilters) => void
  onReset: () => void
}

export function ArticleFiltersForm({ filters, onApply, onReset }: ArticleFiltersFormProps) {
  const [draft, setDraft] = useState(filters)

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    onApply({ ...draft, page: 0 })
  }

  return (
    <form className="filters" onSubmit={handleSubmit}>
      <label>
        <span>类别</span>
        <select
          value={draft.category}
          onChange={(event) => setDraft((current) => ({ ...current, category: event.target.value }))}
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
          type="date"
          value={draft.startDate}
          onChange={(event) => setDraft((current) => ({ ...current, startDate: event.target.value }))}
        />
      </label>
      <label>
        <span>结束日期</span>
        <input
          type="date"
          value={draft.endDate}
          onChange={(event) => setDraft((current) => ({ ...current, endDate: event.target.value }))}
        />
      </label>
      <div className="filter-actions">
        <button className="button primary" type="submit">
          应用筛选
        </button>
        <button className="button secondary" type="button" onClick={onReset}>
          清除
        </button>
      </div>
    </form>
  )
}
