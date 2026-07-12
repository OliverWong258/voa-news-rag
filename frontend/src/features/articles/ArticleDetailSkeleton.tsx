export function ArticleDetailSkeleton() {
  return (
    <article aria-label="正在加载新闻详情" className="article-detail skeleton-detail" role="status">
      <div className="skeleton short" />
      <div className="skeleton detail-title" />
      <div className="skeleton detail-title second" />
      <div className="skeleton line" />
      <div className="skeleton line" />
      <div className="skeleton line" />
      <div className="skeleton line" />
    </article>
  )
}

