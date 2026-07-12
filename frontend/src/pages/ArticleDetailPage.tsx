import { useQuery } from '@tanstack/react-query'
import { Link, useLocation, useParams } from 'react-router-dom'
import { getArticle } from '../api/articles'
import { ApiRequestError } from '../api/http'
import { ArticleDetailSkeleton } from '../features/articles/ArticleDetailSkeleton'
import {
  formatArticleDate,
  safeReturnPath,
  splitArticleContent,
} from '../features/articles/articlePresentation'

interface ArticleLocationState {
  from?: unknown
}

export function ArticleDetailPage() {
  const { articleId = '' } = useParams()
  const location = useLocation()
  const numericArticleId = Number(articleId)
  const validArticleId = /^\d+$/.test(articleId) && Number.isSafeInteger(numericArticleId) && numericArticleId > 0
  const returnPath = safeReturnPath((location.state as ArticleLocationState | null)?.from)
  const query = useQuery({
    queryKey: ['article', numericArticleId],
    queryFn: ({ signal }) => getArticle(numericArticleId, signal),
    enabled: validArticleId,
  })

  if (!validArticleId || (query.error instanceof ApiRequestError && query.error.status === 404)) {
    return (
      <section className="detail-status">
        <p className="eyebrow">404</p>
        <h1>没有找到这篇新闻</h1>
        <p>它可能尚未完成处理，或者已经不存在。</p>
        <Link className="button primary button-link" to={returnPath}>
          返回新闻列表
        </Link>
      </section>
    )
  }

  if (query.isPending) return <ArticleDetailSkeleton />

  if (query.error) {
    const message = query.error instanceof ApiRequestError ? query.error.message : '新闻详情加载失败，请稍后重试。'
    return (
      <section className="detail-status" role="alert">
        <p className="eyebrow">Request failed</p>
        <h1>无法加载新闻</h1>
        <p>{message}</p>
        <div className="detail-actions">
          <button className="button primary" onClick={() => void query.refetch()} type="button">
            重试
          </button>
          <Link className="button secondary button-link" to={returnPath}>
            返回新闻列表
          </Link>
        </div>
      </section>
    )
  }

  const article = query.data
  const paragraphs = splitArticleContent(article.content)

  return (
    <article className="article-detail">
      <Link className="back-link" to={returnPath}>
        <span aria-hidden="true">←</span> 返回新闻列表
      </Link>

      <header className="detail-header">
        <div className="article-meta detail-meta">
          <span className="category-badge">{article.category || '未分类'}</span>
          <time dateTime={article.publishedAt ?? undefined}>{formatArticleDate(article.publishedAt)}</time>
        </div>
        <h1>{article.title}</h1>
        {article.summary && <p className="article-lead">{article.summary}</p>}
        <dl className="article-byline">
          <div>
            <dt>来源</dt>
            <dd>{article.source}</dd>
          </div>
          {article.author && (
            <div>
              <dt>作者</dt>
              <dd>{article.author}</dd>
            </div>
          )}
        </dl>
      </header>

      <div className="article-body">
        {paragraphs.length > 0 ? (
          paragraphs.map((paragraph, index) => <p key={`${index}-${paragraph.slice(0, 24)}`}>{paragraph}</p>)
        ) : (
          <p className="missing-content">这篇新闻暂时没有可显示的正文。</p>
        )}
      </div>

      <footer className="detail-footer">
        <div>
          <strong>查看原始报道</strong>
          <p>在 VOA 网站阅读新闻来源。</p>
        </div>
        <a className="button secondary button-link" href={article.sourceUrl} rel="noreferrer" target="_blank">
          打开 VOA 原文 <span aria-hidden="true">↗</span>
        </a>
      </footer>
    </article>
  )
}

