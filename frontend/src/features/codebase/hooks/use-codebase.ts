import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  getCodebasesApi,
  importCodebaseApi,
  updateCodebaseApi,
  deleteCodebaseApi,
  reindexCodebaseApi,
} from '../api/codebase.api'
import type { ApiResponse } from '#/types/api.types'
import type {
  CodebaseResponse,
  CodebaseImportRequest,
  CodebaseImportResponse,
  CodebaseUpdateRequest,
} from '../types/codebase.types'

export const codebaseQueryKeys = {
  all: ['codebases'] as const,
  detail: (id: string) => [...codebaseQueryKeys.all, id] as const,
}

export function useCodebases() {
  return useQuery<ApiResponse<CodebaseResponse[]>>({
    queryKey: codebaseQueryKeys.all,
    queryFn: getCodebasesApi,
  })
}

export function useImportCodebase() {
  const queryClient = useQueryClient()

  return useMutation<
    ApiResponse<CodebaseImportResponse>,
    Error,
    CodebaseImportRequest
  >({
    mutationFn: importCodebaseApi,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: codebaseQueryKeys.all })
    },
  })
}

export function useUpdateCodebase() {
  const queryClient = useQueryClient()

  return useMutation<
    ApiResponse<CodebaseResponse>,
    Error,
    { codebaseId: string; data: CodebaseUpdateRequest }
  >({
    mutationFn: ({ codebaseId, data }) => updateCodebaseApi(codebaseId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: codebaseQueryKeys.all })
    },
  })
}

export function useDeleteCodebase() {
  const queryClient = useQueryClient()

  return useMutation<void, Error, string>({
    mutationFn: deleteCodebaseApi,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: codebaseQueryKeys.all })
    },
  })
}

export function useReindexCodebase() {
  const queryClient = useQueryClient()

  return useMutation<ApiResponse<CodebaseImportResponse>, Error, string>({
    mutationFn: reindexCodebaseApi,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: codebaseQueryKeys.all })
    },
  })
}
