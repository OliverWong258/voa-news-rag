import { createBrowserRouter } from 'react-router-dom'
import { AppLayout } from '../components/AppLayout'
import { NotFoundPage } from '../pages/NotFoundPage'
import { ArticlesPage } from '../pages/ArticlesPage'
import { ArticleDetailPage } from '../pages/ArticleDetailPage'
import { SearchPage } from '../pages/SearchPage'
import { QaPage } from '../pages/QaPage'

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
        element: <QaPage />,
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
