/** Backend contract for the React client. Dates use YYYY-MM-DD; timestamps use ISO-8601. */
export type LocalDate = string;
export type Instant = string;

export interface ArticlePreview {
  id: number;
  title: string;
  summary: string | null;
  category: string | null;
  source: string;
  sourceUrl: string;
  publishedAt: Instant | null;
}

export interface ArticleDetail extends ArticlePreview {
  content: string;
  author: string | null;
}

export interface ArticlePage {
  items: ArticlePreview[];
  page: number;
  size: number;
  total: number;
}

export interface SearchHit {
  vectorId: string;
  articleId: number;
  chunkId: number;
  chunkIndex: number;
  score: number;
  title: string;
  excerpt: string;
  category: string | null;
  sourceUrl: string;
  publishedAt: Instant | null;
}

export interface NewsSearchResponse {
  query: string;
  total: number;
  hits: SearchHit[];
}

export interface RagQuestionRequest {
  question: string;
  topK?: number;
  category?: string;
  startDate?: LocalDate;
  endDate?: LocalDate;
}

export interface RagSource {
  citation: number;
  articleId: number;
  chunkId: number;
  title: string;
  category: string | null;
  url: string;
  publishedAt: Instant | null;
  score: number;
  excerpt: string;
}

export interface RagAnswerResponse {
  answer: string;
  grounded: boolean;
  sources: RagSource[];
}

export interface RagSseEventMap {
  sources: { sources: RagSource[] };
  token: { text: string };
  completed: { grounded: boolean };
}

export type RagSseEvent = {
  [K in keyof RagSseEventMap]: { event: K; data: RagSseEventMap[K] };
}[keyof RagSseEventMap];

export interface ApiError {
  timestamp: Instant;
  status: number;
  code: string;
  message: string;
  path: string;
}

export interface ArticleListParams {
  category?: string;
  startDate?: LocalDate;
  endDate?: LocalDate;
  page?: number;
  size?: number;
}

export interface SearchParams {
  q: string;
  topK?: number;
  category?: string;
  startDate?: LocalDate;
  endDate?: LocalDate;
}
