import { fetchEventSource } from '@microsoft/fetch-event-source'
import { apiClient } from '#/lib/api-client'
import { tokenManager } from '#/lib/token-manager'
import { refreshApi } from '#/features/auth'
import { env } from '#/env'
import type { ApiResponse } from '#/types/api.types'
import type {
  ChatSessionResponse,
  ChatSessionUpdateRequest,
  CodeChatRequest,
  CodeCitation,
  ChatHistoryResponse,
} from '../types/chat.types'

export interface GetChatMessagesParams {
  limit?: number
  before?: string
}

export async function getChatMessagesApi(
  codebaseId: string,
  sessionId: string,
  params?: GetChatMessagesParams,
): Promise<ApiResponse<ChatHistoryResponse>> {
  const searchParams = new URLSearchParams()
  if (params?.limit != null) searchParams.set('limit', String(params.limit))
  if (params?.before) searchParams.set('before', params.before)
  const qs = searchParams.toString()
  const url = `/codebases/${codebaseId}/chat/sessions/${sessionId}/messages${qs ? `?${qs}` : ''}`
  const response = await apiClient.get<ApiResponse<ChatHistoryResponse>>(url)
  return response.data
}

export async function getChatSessionsApi(
  codebaseId: string,
): Promise<ApiResponse<ChatSessionResponse[]>> {
  const response = await apiClient.get<ApiResponse<ChatSessionResponse[]>>(
    `/codebases/${codebaseId}/chat/sessions`,
  )
  return response.data
}

export async function updateChatSessionApi(
  codebaseId: string,
  sessionId: string,
  data: ChatSessionUpdateRequest,
): Promise<ApiResponse<ChatSessionResponse>> {
  const response = await apiClient.patch<ApiResponse<ChatSessionResponse>>(
    `/codebases/${codebaseId}/chat/sessions/${sessionId}`,
    data,
  )
  return response.data
}

export async function deleteChatSessionApi(
  codebaseId: string,
  sessionId: string,
): Promise<void> {
  await apiClient.delete(`/codebases/${codebaseId}/chat/sessions/${sessionId}`)
}

const baseUrl = env.VITE_API_BASE_URL.replace(/\/+$/, '')

export interface StreamChatCallbacks {
  onMessage: (chunk: string) => void
  onCitations: (citations: CodeCitation[]) => void
  onTitle?: (title: string) => void
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
            Accept: 'text/event-stream',
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
                    } else if (
                      parsed &&
                      typeof parsed === 'object' &&
                      parsed !== null
                    ) {
                      if (
                        'content' in parsed &&
                        typeof parsed.content === 'string'
                      ) {
                        chunkText = parsed.content
                      } else if (
                        'message' in parsed &&
                        typeof parsed.message === 'string'
                      ) {
                        chunkText = parsed.message
                      } else if (
                        'text' in parsed &&
                        typeof parsed.text === 'string'
                      ) {
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
              case 'title':
                try {
                  const title: string = JSON.parse(event.data)
                  callbacks.onTitle?.(title)
                } catch {
                  callbacks.onTitle?.(event.data)
                }
                break
              case 'done':
                try {
                  const resolvedChatId: string = JSON.parse(event.data)
                  callbacks.onDone(resolvedChatId)
                } catch {
                  callbacks.onDone(event.data)
                }
                ctrl.abort()
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
                ctrl.abort()
                break
            }
          },

          onerror: (error) => {
            callbacks.onError(
              error instanceof Error
                ? error.message
                : 'Stream connection failed',
            )
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
