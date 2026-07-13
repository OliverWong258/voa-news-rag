import { useQuery } from '@tanstack/react-query'
import { useLocation, useSearchParams } from 'react-router-dom'
import { ApiRequestError } from '../api/http'
import { searchNews } from '../api/search'
import { SearchForm } from '../features/search/SearchForm'
import { SearchResultCard } from '../features/search/SearchResultCard'
import { SearchResultsSkeleton } from '../features/search/SearchResultsSkeleton'
import {
  DEFAULT_SEARCH_FILTERS,
  readSearchFilters,
  toSearchParams,
  validateSearchFilters,
  writeSearchFilters,
} from '../features/search/searchFilters'

export function SearchPage() {
  const location = useLocation()
  const [searchParams, setSearchParams] = useSearchParams()
  const filters = readSearchFilters(searchParams)
  const hasSubmittedQuery = searchParams.has('q')
  const validationError = hasSubmittedQuery ? validateSearchFilters(filters) : null
  const query = useQuery({
    queryKey: ['search', filters],
    queryFn: ({ signal }) => searchNews(toSearchParams(filters), signal),
    enabled: hasSubmittedQuery && validationError === null,
  })
  const returnPath = `${location.pathname}${location.search}`
  const errorMessage =
    validationError ??
    (query.error instanceof ApiRequestError ? query.error.message : query.error ? '搜索失败，请稍后重试。' : null)

  return (
    <section className="search-page">
      <header className="search-heading">
        <p className="eyebrow">Semantic discovery</p>
        <h1>智能搜索</h1>
        <p>按语义查找新闻及最相关的中文文本片段，不必精确匹配原文关键词。</p>
      </header>

      <SearchForm
        filters={filters}
        key={searchParams.toString()}
        onClear={() => setSearchParams(writeSearchFilters(DEFAULT_SEARCH_FILTERS))}
        onSearch={(nextFilters) => setSearchParams(writeSearchFilters(nextFilters))}
      />

      {!hasSubmittedQuery && (
        <div className="search-welcome">
          <strong>从一个问题或主题开始</strong>
          <p>例如搜索“美国对亚洲的外交政策”或“非洲近期选举”。</p>
        </div>
      )}

      {errorMessage && (
        <div className="status-panel error" role="alert">
          <div>
            <strong>无法完成搜索</strong>
            <p>{errorMessage}</p>
          </div>
          {!validationError && (
            <button className="button secondary" onClick={() => void query.refetch()} type="button">
              重试
            </button>
          )}
        </div>
      )}

      {query.isPending && hasSubmittedQuery && !errorMessage && <SearchResultsSkeleton />}

      {query.data && !errorMessage && (
        <div className="search-response">
          <div className="search-summary">
            <span>
              “<strong>{query.data.query}</strong>”的搜索结果
            </span>
            <span>{query.data.total} 个相关片段</span>
          </div>

          {query.data.hits.length === 0 ? (
            <div className="status-panel empty">
              <div>
                <strong>没有找到相关内容</strong>
                <p>尝试简化搜索内容、扩大日期范围或取消类别限制。</p>
              </div>
            </div>
          ) : (
            <div className="search-results">
              {query.data.hits.map((hit) => (
                <SearchResultCard
                  hit={hit}
                  key={hit.vectorId}
                  query={query.data.query}
                  returnPath={returnPath}
                />
              ))}
            </div>
          )}
        </div>
      )}
    </section>
  )
}

