import { Link } from 'react-router-dom'
import type { SearchHit } from '../../api/contracts'
import { formatArticleDate } from '../articles/articlePresentation'
import { highlightedText, relevancePercent } from './searchPresentation'

interface SearchResultCardProps {
  hit: SearchHit
  query: string
  returnPath: string
}

export function SearchResultCard({ hit, query, returnPath }: SearchResultCardProps) {
  const relevance = relevancePercent(hit.score)

  return (
    <article className="search-result-card">
      <div className="search-result-main">
        <div className="article-meta search-result-meta">
          <span className="category-badge">{hit.category || '未分类'}</span>
          <time dateTime={hit.publishedAt ?? undefined}>{formatArticleDate(hit.publishedAt)}</time>
        </div>
        <h2>
          <Link state={{ from: returnPath }} to={`/articles/${hit.articleId}`}>
            {highlightedText(hit.title, query)}
          </Link>
        </h2>
        <blockquote>{highlightedText(hit.excerpt, query)}</blockquote>
        <Link className="read-more" state={{ from: returnPath }} to={`/articles/${hit.articleId}`}>
          查看完整新闻 <span aria-hidden="true">→</span>
        </Link>
      </div>
      <div aria-label={`相关度 ${relevance}%`} className="relevance">
        <strong>{relevance}%</strong>
        <span>相关度</span>
        <div className="relevance-track">
          <div className="relevance-value" style={{ width: `${relevance}%` }} />
        </div>
      </div>
    </article>
  )
}
