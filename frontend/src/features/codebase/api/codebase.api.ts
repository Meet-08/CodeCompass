import { fetchEventSource } from '@microsoft/fetch-event-source'
import { apiClient } from '#/lib/api-client'
import { tokenManager } from '#/lib/token-manager'
import { refreshApi } from '#/features/auth'
import { env } from '#/env'
import type { ApiResponse } from '#/types/api.types'
import type {
  CodebaseResponse,
  CodebaseImportRequest,
  CodebaseImportResponse,
  CodebaseUpdateRequest,
  CodeChatRequest,
  CodeCitation,
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

const baseUrl = env.VITE_API_BASE_URL.replace(/\/+$/, '')

export interface StreamChatCallbacks {
  onMessage: (chunk: string) => void
  onCitations: (citations: CodeCitation[]) => void
  onDone: (chatId: string) => void
  onError: (message: string) => void
}

/**
 * Opens a POST-based SSE connection to the chat stream endpoint.
 * Proactively checks/refreshes the access token if needed before initiating the stream.
 * Returns an AbortController so the caller can cancel the stream.
 */
export function streamChatApi(
  codebaseId: string,
  data: CodeChatRequest,
  callbacks: StreamChatCallbacks,
): AbortController {
  const ctrl = new AbortController()

  const startStream = async () => {
    let token = tokenManager.getAccessToken()

    // If no access token in memory, attempt refresh using HttpOnly cookie first
    if (!token) {
      try {
        const refreshRes = await refreshApi()
        if (refreshRes.success && refreshRes.data?.accessToken) {
          tokenManager.setAccessToken(refreshRes.data.accessToken)
          token = refreshRes.data.accessToken
        }
      } catch {
        // Continue to attempt stream request
      }
    }

    try {
      await fetchEventSource(
        `${baseUrl}/api/codebases/${codebaseId}/chat/stream`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
          body: JSON.stringify(data),
          signal: ctrl.signal,

          onopen: async (response) => {
            if (!response.ok) {
              const errorBody = await response.json().catch(() => null)
              const message =
                errorBody &&
                typeof errorBody === 'object' &&
                'message' in errorBody &&
                typeof errorBody.message === 'string'
                  ? errorBody.message
                  : `Request failed with status ${response.status}`
              throw new Error(message)
            }
          },

          onmessage: (event) => {
            switch (event.event) {
              case 'message': {
                let chunkText = event.data
                if (typeof event.data === 'string') {
                  try {
                    const parsed = JSON.parse(event.data)
                    if (typeof parsed === 'string') {
                      chunkText = parsed
                    } else if (parsed && typeof parsed === 'object' && parsed !== null) {
                      if ('content' in parsed && typeof parsed.content === 'string') {
                        chunkText = parsed.content
                      } else if ('message' in parsed && typeof parsed.message === 'string') {
                        chunkText = parsed.message
                      } else if ('text' in parsed && typeof parsed.text === 'string') {
                        chunkText = parsed.text
                      }
                    }
                  } catch {
                    // Keep raw event.data
                  }
                }
                callbacks.onMessage(chunkText)
                break
              }
              case 'citations':
                try {
                  const citations: CodeCitation[] = JSON.parse(event.data)
                  callbacks.onCitations(citations)
                } catch {
                  callbacks.onError('Failed to parse citations')
                }
                break
              case 'done':
                callbacks.onDone(event.data)
                break
              case 'error':
                try {
                  const errorPayload = JSON.parse(event.data) as {
                    message: string
                  }
                  callbacks.onError(errorPayload.message)
                } catch {
                  callbacks.onError(event.data || 'Unknown stream error')
                }
                break
            }
          },

          onerror: (error) => {
            callbacks.onError(
              error instanceof Error
                ? error.message
                : 'Stream connection failed',
            )
            // Throw to prevent automatic retry from the library
            throw error
          },

          openWhenHidden: true,
        },
      )
    } catch (err) {
      if (!ctrl.signal.aborted) {
        callbacks.onError(
          err instanceof Error ? err.message : 'Stream request failed',
        )
      }
    }
  }

  void startStream()

  return ctrl
}
