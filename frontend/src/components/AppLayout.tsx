import { NavLink, Outlet } from 'react-router-dom'

const navigation = [
  { to: '/', label: '新闻' },
  { to: '/search', label: '智能搜索' },
  { to: '/ask', label: '智能问答' },
]

export function AppLayout() {
  return (
    <div className="app-shell">
      <header className="site-header">
        <NavLink className="brand" to="/">
          VOA 智能新闻
        </NavLink>
        <nav aria-label="主导航">
          {navigation.map((item) => (
            <NavLink
              className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
              end={item.to === '/'}
              key={item.to}
              to={item.to}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </header>
      <main className="page-container">
        <Outlet />
      </main>
    </div>
  )
}

