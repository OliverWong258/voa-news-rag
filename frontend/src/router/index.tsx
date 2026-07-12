import { createBrowserRouter } from 'react-router-dom'
import { AppLayout } from '../components/AppLayout'
import { PlaceholderPage } from '../pages/PlaceholderPage'
import { NotFoundPage } from '../pages/NotFoundPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      {
        index: true,
        element: (
          <PlaceholderPage
            title="新闻列表"
            description="浏览 VOA 中文新闻，并按日期和类别筛选。"
          />
        ),
      },
      {
        path: 'articles/:articleId',
        element: (
          <PlaceholderPage
            title="新闻详情"
            description="查看新闻译文、来源和发布时间。"
          />
        ),
      },
      {
        path: 'search',
        element: (
          <PlaceholderPage
            title="智能搜索"
            description="通过语义搜索定位相关新闻与文本片段。"
          />
        ),
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

