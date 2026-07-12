# Path To Nowhere Strategy Frontend

React + TypeScript 前端，使用 Vite 构建。

## 本地开发

```powershell
npm install
npm run dev
```

开发服务器会将 `/api` 请求代理到 `http://localhost:8080`。可以通过
`VITE_API_PROXY_TARGET` 修改代理目标。

## 常用命令

```powershell
npm run typecheck
npm test
npm run build
```

后端 API 契约位于 `../docs/frontend-api.md` 和 `../docs/frontend-api.ts`。

