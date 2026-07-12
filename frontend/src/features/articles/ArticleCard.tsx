import { Link, useLocation } from 'react-router-dom'
import type { ArticlePreview } from '../../api/contracts'
import { formatArticleDate } from './articlePresentation'

export function ArticleCard({ article }: { article: ArticlePreview }) {
  const location = useLocation()
  const linkState = { from: `${location.pathname}${location.search}` }

  return (
    <article className="article-card">
      <div className="article-meta">
        <span className="category-badge">{article.category || '未分类'}</span>
        <time dateTime={article.publishedAt ?? undefined}>{formatArticleDate(article.publishedAt)}</time>
      </div>
      <h2>
        <Link state={linkState} to={`/articles/${article.id}`}>
          {article.title}
        </Link>
      </h2>
      <p>{article.summary || '这篇新闻暂时没有摘要。'}</p>
      <div className="article-footer">
        <span>来源：{article.source}</span>
        <Link className="read-more" state={linkState} to={`/articles/${article.id}`}>
          阅读全文 <span aria-hidden="true">→</span>
        </Link>
      </div>
    </article>
  )
}
