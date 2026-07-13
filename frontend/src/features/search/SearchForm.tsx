import type { FormEvent } from 'react'
import { useState } from 'react'
import { CATEGORIES } from '../../config/categories'
import { SEARCH_RESULT_LIMITS, type SearchFilters } from './searchFilters'

interface SearchFormProps {
  filters: SearchFilters
  onSearch: (filters: SearchFilters) => void
  onClear: () => void
}

export function SearchForm({ filters, onSearch, onClear }: SearchFormProps) {
  const [draft, setDraft] = useState(filters)

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    onSearch({ ...draft, q: draft.q.trim() })
  }

  return (
    <form className="search-form" onSubmit={handleSubmit}>
      <div className="search-input-row">
        <label className="search-query-label">
          <span className="sr-only">搜索新闻</span>
          <input
            autoFocus
            maxLength={500}
            placeholder="输入事件、人物或主题，例如：美国大选"
            type="search"
            value={draft.q}
            onChange={(event) => setDraft((current) => ({ ...current, q: event.target.value }))}
          />
        </label>
        <button className="button primary search-button" type="submit">
          智能搜索
        </button>
      </div>

      <div className="search-filter-row">
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
        <label>
          <span>结果数量</span>
          <select
            value={draft.topK}
            onChange={(event) => setDraft((current) => ({ ...current, topK: Number(event.target.value) }))}
          >
            {SEARCH_RESULT_LIMITS.map((limit) => (
              <option key={limit} value={limit}>
                {limit} 条
              </option>
            ))}
          </select>
        </label>
        <button className="clear-search" onClick={onClear} type="button">
          清除条件
        </button>
      </div>
    </form>
  )
}

