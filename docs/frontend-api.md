# Frontend API contract

All JSON endpoints use `application/json`. Date filters are inclusive and use `YYYY-MM-DD`.

| Feature | Method and path | Contract |
|---|---|---|
| News previews | `GET /api/articles` | `ArticleListParams -> ArticlePage` |
| News detail | `GET /api/articles/{articleId}` | `ArticleDetail` |
| Semantic search | `GET /api/search` | `SearchParams -> NewsSearchResponse` |
| Synchronous Q&A | `POST /api/qa` | `RagQuestionRequest -> RagAnswerResponse` |
| Streaming Q&A | `POST /api/qa/stream` | `RagQuestionRequest -> text/event-stream` |

The streaming endpoint emits events in this order:

1. `sources`: retrieved citations and preview excerpts.
2. `token`: repeated incremental answer text.
3. `completed`: contains the final `grounded` flag.

Use `fetch` plus `ReadableStream` for streaming because the browser's native `EventSource`
does not support a POST request body. Append every `token.data.text` value in arrival order.
The source citation `[n]` in answer text maps to the source whose `citation` equals `n`.

The canonical TypeScript declarations are in [`frontend-api.ts`](./frontend-api.ts).
