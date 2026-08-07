import { apiClient } from '#/lib/api-client'
import type { ApiResponse } from '#/types/api.types'
import type {
  CodebaseResponse,
  CodebaseImportRequest,
  CodebaseImportResponse,
  CodebaseUpdateRequest,
} from '../types/codebase.types'

export async function importCodebaseApi(
  data: CodebaseImportRequest,
): Promise<ApiResponse<CodebaseImportResponse>> {
  const response = await apiClient.post<ApiResponse<CodebaseImportResponse>>(
    '/codebases',
    data,
  )
  return response.data
}

export async function getCodebasesApi(): Promise<
  ApiResponse<CodebaseResponse[]>
> {
  const response =
    await apiClient.get<ApiResponse<CodebaseResponse[]>>('/codebases')
  return response.data
}

export async function updateCodebaseApi(
  codebaseId: string,
  data: CodebaseUpdateRequest,
): Promise<ApiResponse<CodebaseResponse>> {
  const response = await apiClient.patch<ApiResponse<CodebaseResponse>>(
    `/codebases/${codebaseId}`,
    data,
  )
  return response.data
}

export async function deleteCodebaseApi(codebaseId: string): Promise<void> {
  await apiClient.delete(`/codebases/${codebaseId}`)
}

export async function reindexCodebaseApi(
  codebaseId: string,
): Promise<ApiResponse<CodebaseImportResponse>> {
  const response = await apiClient.post<ApiResponse<CodebaseImportResponse>>(
    `/codebases/${codebaseId}/reindex`,
  )
  return response.data
}
