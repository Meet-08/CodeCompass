import axios from 'axios'
import { env } from '#/env'
import { tokenManager } from './token-manager'

const baseUrl = env.VITE_API_BASE_URL.replace(/\/+$/, '')

export const apiClient = axios.create({
  baseURL: `${baseUrl}/api`,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Axios request interceptor to attach Bearer token from TokenManager
apiClient.interceptors.request.use((config) => {
  const token = tokenManager.getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Axios response interceptor for automatic 401 token refresh
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    // Exclude auth endpoints from auto-retry loop to avoid infinite recursion
    const isAuthEndpoint =
      originalRequest?.url?.includes('/auth/login') ||
      originalRequest?.url?.includes('/auth/register') ||
      originalRequest?.url?.includes('/auth/refresh')

    if (
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !isAuthEndpoint
    ) {
      originalRequest._retry = true
      try {
        const refreshResponse = await apiClient.post('/auth/refresh')
        const newAccessToken = refreshResponse.data?.data?.accessToken
        if (newAccessToken) {
          tokenManager.setAccessToken(newAccessToken)
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
        }
        return apiClient(originalRequest)
      } catch (refreshError) {
        tokenManager.clearAccessToken()
        return Promise.reject(refreshError)
      }
    }

    return Promise.reject(error)
  }
)
