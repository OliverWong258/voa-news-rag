export function SearchResultsSkeleton() {
  return (
    <div aria-label="正在搜索新闻" className="search-results" role="status">
      {Array.from({ length: 4 }, (_, index) => (
        <div className="search-result-card skeleton-result" key={index}>
          <div className="skeleton short" />
          <div className="skeleton heading" />
          <div className="skeleton line" />
          <div className="skeleton line" />
        </div>
      ))}
    </div>
  )
}

