import type { ArticleDetail, ArticleListParams, ArticlePage } from './contracts'
import { requestJson } from './http'
import { toQueryString } from './query'

export function getArticles(params: ArticleListParams = {}, signal?: AbortSignal) {
  return requestJson<ArticlePage>(`/api/articles${toQueryString(params)}`, { signal })
}

export function getArticle(articleId: number, signal?: AbortSignal) {
  return requestJson<ArticleDetail>(`/api/articles/${articleId}`, { signal })
}

