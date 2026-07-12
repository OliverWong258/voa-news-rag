import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { getArticles } from '../api/articles'
import { ApiRequestError } from '../api/http'
import { ArticleCard } from '../features/articles/ArticleCard'
import { ArticleFiltersForm } from '../features/articles/ArticleFiltersForm'
import { ArticleListSkeleton } from '../features/articles/ArticleListSkeleton'
import { Pagination } from '../features/articles/Pagination'
import {
  DEFAULT_ARTICLE_FILTERS,
  readArticleFilters,
  toArticleListParams,
  validateDateRange,
  writeArticleFilters,
} from '../features/articles/articleFilters'

export function ArticlesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const filters = readArticleFilters(searchParams)
  const validationError = validateDateRange(filters)
  const query = useQuery({
    queryKey: ['articles', filters],
    queryFn: ({ signal }) => getArticles(toArticleListParams(filters), signal),
    enabled: validationError === null,
    placeholderData: keepPreviousData,
  })

  const errorMessage =
    validationError ??
    (query.error instanceof ApiRequestError ? query.error.message : query.error ? '新闻加载失败，请稍后重试。' : null)

  function changePage(page: number) {
    setSearchParams(writeArticleFilters({ ...filters, page }))
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  return (
    <section>
      <header className="page-heading">
        <div>
          <p className="eyebrow">Translated journalism</p>
          <h1>VOA 中文新闻</h1>
          <p>浏览经过翻译和索引的 VOA 新闻，按主题或发布日期缩小范围。</p>
        </div>
        {query.data && <span className="result-count">共 {query.data.total} 篇新闻</span>}
      </header>

      <ArticleFiltersForm
        filters={filters}
        key={searchParams.toString()}
        onApply={(nextFilters) => setSearchParams(writeArticleFilters(nextFilters))}
        onReset={() => setSearchParams(writeArticleFilters(DEFAULT_ARTICLE_FILTERS))}
      />

      {errorMessage && (
        <div className="status-panel error" role="alert">
          <div>
            <strong>无法加载新闻</strong>
            <p>{errorMessage}</p>
          </div>
          {!validationError && (
            <button className="button secondary" onClick={() => void query.refetch()} type="button">
              重试
            </button>
          )}
        </div>
      )}

      {query.isPending && !errorMessage && <ArticleListSkeleton />}

      {query.data && query.data.items.length === 0 && !errorMessage && (
        <div className="status-panel empty">
          <div>
            <strong>没有符合条件的新闻</strong>
            <p>尝试调整日期范围或选择其他类别。</p>
          </div>
          <button
            className="button secondary"
            onClick={() => setSearchParams(writeArticleFilters(DEFAULT_ARTICLE_FILTERS))}
            type="button"
          >
            清除筛选
          </button>
        </div>
      )}

      {query.data && query.data.items.length > 0 && (
        <>
          <div className={`article-grid${query.isPlaceholderData ? ' refreshing' : ''}`}>
            {query.data.items.map((article) => (
              <ArticleCard article={article} key={article.id} />
            ))}
          </div>
          <Pagination
            page={query.data.page}
            size={query.data.size}
            total={query.data.total}
            onPageChange={changePage}
          />
        </>
      )}
    </section>
  )
}
