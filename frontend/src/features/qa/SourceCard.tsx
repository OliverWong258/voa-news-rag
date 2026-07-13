import { Link } from 'react-router-dom'
import type { RagSource } from '../../api/contracts'
import { formatArticleDate } from '../articles/articlePresentation'
import { relevancePercent } from '../search/searchPresentation'

export function SourceCard({ source }: { source: RagSource }) {
  return (
    <article className="source-card" id={`source-${source.citation}`}>
      <div className="source-citation">[{source.citation}]</div>
      <div className="source-content">
        <div className="article-meta source-meta">
          <span className="category-badge">{source.category || '未分类'}</span>
          <time dateTime={source.publishedAt ?? undefined}>{formatArticleDate(source.publishedAt)}</time>
          <span>{relevancePercent(source.score)}% 相关</span>
        </div>
        <h3>{source.title}</h3>
        <p>{source.excerpt}</p>
        <div className="source-links">
          <Link to={`/articles/${source.articleId}`}>查看新闻</Link>
          <a href={source.url} rel="noreferrer" target="_blank">
            VOA 原文 <span aria-hidden="true">↗</span>
          </a>
        </div>
      </div>
    </article>
  )
}

