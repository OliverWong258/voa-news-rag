export function ArticleListSkeleton() {
  return (
    <div aria-label="正在加载新闻" className="article-grid" role="status">
      {Array.from({ length: 6 }, (_, index) => (
        <div className="article-card skeleton-card" key={index}>
          <div className="skeleton short" />
          <div className="skeleton heading" />
          <div className="skeleton line" />
          <div className="skeleton line" />
        </div>
      ))}
    </div>
  )
}

