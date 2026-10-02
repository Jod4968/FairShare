const API_URL = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api').replace(/\/$/, '')

export async function apiRequest<T>(path: string, options: RequestInit & { token?: string } = {}): Promise<T> {
  const { token, ...requestOptions } = options
  const headers = new Headers(requestOptions.headers)
  headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)
  const response = await fetch(`${API_URL}${path}`, { ...requestOptions, headers })
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { error?: string; message?: string } | null
    throw new Error(body?.error ?? body?.message ?? 'Something went wrong. Please try again.')
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export type AuthResponse = { token: string; user?: { email: string; fullName?: string } }
export type Group = { id: number; name: string; joinCode: string; createdBy: number; createdAt: string; updatedAt: string }
export type GroupMember = { userId: number; email: string; fullName: string; joinedAt: string }

export const authApi = {
  login: (email: string, password: string) => apiRequest<AuthResponse>('/auth/login', {
    method: 'POST', body: JSON.stringify({ email, password }),
  }),
  register: (name: string, email: string, password: string) => apiRequest<AuthResponse>('/auth/register', {
    method: 'POST', body: JSON.stringify({ name, email, password }),
  }),
}

export const groupsApi = {
  list: (token: string) => apiRequest<Group[]>('/groups', { token }),
  create: (name: string, token: string) => apiRequest<Group>('/groups', { method: 'POST', token, body: JSON.stringify({ name }) }),
  join: (joinCode: string, token: string) => apiRequest<Group>('/groups/join', { method: 'POST', token, body: JSON.stringify({ joinCode }) }),
  get: (id: number, token: string) => apiRequest<Group>(`/groups/${id}`, { token }),
  members: (id: number, token: string) => apiRequest<GroupMember[]>(`/groups/${id}/members`, { token }),
  leave: (id: number, token: string) => apiRequest<void>(`/groups/${id}/membership`, { method: 'DELETE', token }),
}
