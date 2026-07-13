import type { ApiError } from './contracts'

export class ApiRequestError extends Error {
  readonly status: number
  readonly code: string
  readonly path?: string

  constructor(message: string, status: number, code = 'HTTP_ERROR', path?: string) {
    super(message)
    this.name = 'ApiRequestError'
    this.status = status
    this.code = code
    this.path = path
  }
}

function isApiError(value: unknown): value is ApiError {
  if (!value || typeof value !== 'object') return false
  const candidate = value as Partial<ApiError>
  return (
    typeof candidate.status === 'number' &&
    typeof candidate.code === 'string' &&
    typeof candidate.message === 'string'
  )
}

export async function apiErrorFromResponse(response: Response): Promise<ApiRequestError> {
  const body: unknown = await response.json().catch(() => null)
  if (isApiError(body)) {
    return new ApiRequestError(body.message, body.status, body.code, body.path)
  }
  return new ApiRequestError(response.statusText || '请求失败', response.status)
}

export async function requestJson<T>(input: RequestInfo | URL, init?: RequestInit): Promise<T> {
  const response = await fetch(input, {
    ...init,
    headers: {
      Accept: 'application/json',
      ...init?.headers,
    },
  })

  if (!response.ok) {
    throw await apiErrorFromResponse(response)
  }

  return response.json() as Promise<T>
}
