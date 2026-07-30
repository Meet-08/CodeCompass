import { apiClient } from '#/lib/api-client'
import type { ApiResponse } from '#/types/api.types'
import type {
  AuthResponse,
  LoginInput,
  RegisterInput,
  UserResponse,
} from '../types/auth.types'

export async function registerApi(
  data: RegisterInput
): Promise<ApiResponse<AuthResponse>> {
  const response = await apiClient.post<ApiResponse<AuthResponse>>(
    '/auth/register',
    data
  )
  return response.data
}

export async function loginApi(
  data: LoginInput
): Promise<ApiResponse<AuthResponse>> {
  const response = await apiClient.post<ApiResponse<AuthResponse>>(
    '/auth/login',
    data
  )
  return response.data
}

export async function refreshApi(): Promise<ApiResponse<AuthResponse>> {
  const response = await apiClient.post<ApiResponse<AuthResponse>>(
    '/auth/refresh'
  )
  return response.data
}

export async function logoutApi(): Promise<ApiResponse<null>> {
  const response = await apiClient.post<ApiResponse<null>>('/auth/logout')
  return response.data
}

export async function getCurrentUserApi(): Promise<ApiResponse<UserResponse>> {
  const response = await apiClient.get<ApiResponse<UserResponse>>('/auth/me')
  return response.data
}
