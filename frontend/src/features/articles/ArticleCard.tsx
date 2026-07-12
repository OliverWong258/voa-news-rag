import { Link } from 'react-router-dom'
import type { ArticlePreview } from '../../api/contracts'

const dateFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
})

function formatPublishedAt(value: string | null) {
  if (!value) return '发布时间未知'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : dateFormatter.format(date)
}

export function ArticleCard({ article }: { article: ArticlePreview }) {
  return (
    <article className="article-card">
      <div className="article-meta">
        <span className="category-badge">{article.category || '未分类'}</span>
        <time dateTime={article.publishedAt ?? undefined}>{formatPublishedAt(article.publishedAt)}</time>
      </div>
      <h2>
        <Link to={`/articles/${article.id}`}>{article.title}</Link>
      </h2>
      <p>{article.summary || '这篇新闻暂时没有摘要。'}</p>
      <div className="article-footer">
        <span>来源：{article.source}</span>
        <Link className="read-more" to={`/articles/${article.id}`}>
          阅读全文 <span aria-hidden="true">→</span>
        </Link>
      </div>
    </article>
  )
}

