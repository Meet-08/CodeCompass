import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { UseQueryOptions } from '@tanstack/react-query'
import {
  getCurrentUserApi,
  loginApi,
  logoutApi,
  refreshApi,
  registerApi,
} from '../api/auth.api'
import type {
  AuthResponse,
  LoginInput,
  RegisterInput,
  UserResponse,
} from '../types/auth.types'
import type { ApiResponse } from '#/types/api.types'
import { tokenManager } from '#/lib/token-manager'

export const authQueryKeys = {
  all: ['auth'] as const,
  me: () => [...authQueryKeys.all, 'me'] as const,
}

export function useCurrentUser(
  options?: Partial<UseQueryOptions<ApiResponse<UserResponse>, Error>>,
) {
  return useQuery<ApiResponse<UserResponse>, Error>({
    queryKey: authQueryKeys.me(),
    queryFn: getCurrentUserApi,
    retry: false,
    staleTime: 1000 * 60 * 10, // 10 minutes
    ...options,
  })
}

export function useLogin() {
  const queryClient = useQueryClient()

  return useMutation<ApiResponse<AuthResponse>, Error, LoginInput>({
    mutationFn: loginApi,
    onSuccess: (data) => {
      if (data.success && data.data) {
        if (data.data.accessToken) {
          tokenManager.setAccessToken(data.data.accessToken)
        }
        queryClient.setQueryData<ApiResponse<UserResponse>>(
          authQueryKeys.me(),
          {
            success: true,
            message: data.message,
            data: data.data.user,
          },
        )
      }
    },
  })
}

export function useRegister() {
  const queryClient = useQueryClient()

  return useMutation<ApiResponse<AuthResponse>, Error, RegisterInput>({
    mutationFn: registerApi,
    onSuccess: (data) => {
      if (data.success && data.data) {
        if (data.data.accessToken) {
          tokenManager.setAccessToken(data.data.accessToken)
        }
        queryClient.setQueryData<ApiResponse<UserResponse>>(
          authQueryKeys.me(),
          {
            success: true,
            message: data.message,
            data: data.data.user,
          },
        )
      }
    },
  })
}

export function useRefreshToken() {
  const queryClient = useQueryClient()

  return useMutation<ApiResponse<AuthResponse>, Error, void>({
    mutationFn: refreshApi,
    onSuccess: (data) => {
      if (data.success && data.data) {
        if (data.data.accessToken) {
          tokenManager.setAccessToken(data.data.accessToken)
        }
        queryClient.setQueryData<ApiResponse<UserResponse>>(
          authQueryKeys.me(),
          {
            success: true,
            message: data.message,
            data: data.data.user,
          },
        )
      }
    },
  })
}

export function useLogout() {
  const queryClient = useQueryClient()

  return useMutation<ApiResponse<null>, Error, void>({
    mutationFn: logoutApi,
    onSuccess: () => {
      tokenManager.clearAccessToken()
      queryClient.setQueryData(authQueryKeys.me(), null)
      queryClient.invalidateQueries({ queryKey: authQueryKeys.all })
    },
    onError: () => {
      tokenManager.clearAccessToken()
      queryClient.setQueryData(authQueryKeys.me(), null)
    },
  })
}
