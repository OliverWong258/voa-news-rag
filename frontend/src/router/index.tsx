import { createBrowserRouter } from 'react-router-dom'
import { AppLayout } from '../components/AppLayout'
import { PlaceholderPage } from '../pages/PlaceholderPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { ArticlesPage } from '../pages/ArticlesPage'
import { ArticleDetailPage } from '../pages/ArticleDetailPage'
import { SearchPage } from '../pages/SearchPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      {
        index: true,
        element: <ArticlesPage />,
      },
      {
        path: 'articles/:articleId',
        element: <ArticleDetailPage />,
      },
      {
        path: 'search',
        element: <SearchPage />,
      },
      {
        path: 'ask',
        element: (
          <PlaceholderPage
            title="智能问答"
            description="基于新闻资料获得带引用来源的流式回答。"
          />
        ),
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
