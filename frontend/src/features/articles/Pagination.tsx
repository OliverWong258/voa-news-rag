interface PaginationProps {
  page: number
  size: number
  total: number
  onPageChange: (page: number) => void
}

export function Pagination({ page, size, total, onPageChange }: PaginationProps) {
  const pageCount = Math.ceil(total / size)
  if (pageCount <= 1) return null

  return (
    <nav aria-label="新闻分页" className="pagination">
      <button
        className="button secondary"
        disabled={page === 0}
        onClick={() => onPageChange(page - 1)}
        type="button"
      >
        上一页
      </button>
      <span>
        第 {page + 1} 页，共 {pageCount} 页
      </span>
      <button
        className="button secondary"
        disabled={page + 1 >= pageCount}
        onClick={() => onPageChange(page + 1)}
        type="button"
      >
        下一页
      </button>
    </nav>
  )
}

