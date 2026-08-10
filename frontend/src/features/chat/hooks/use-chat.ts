import { useRef, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  getChatSessionsApi,
  getChatMessagesApi,
  updateChatSessionApi,
  deleteChatSessionApi,
  streamChatApi,
} from '../api/chat.api'
import type { ApiResponse } from '#/types/api.types'
import type {
  ChatSessionResponse,
  ChatSessionUpdateRequest,
  ChatHistoryResponse,
  CodeChatRequest,
  CodeCitation,
} from '../types/chat.types'

export const chatQueryKeys = {
  all: ['chat'] as const,
  sessions: (codebaseId: string) =>
    [...chatQueryKeys.all, 'sessions-list', codebaseId] as const,
  session: (codebaseId: string, sessionId: string) =>
    [...chatQueryKeys.all, 'session-detail', codebaseId, sessionId] as const,
  messages: (codebaseId: string, sessionId: string) =>
    [...chatQueryKeys.session(codebaseId, sessionId), 'messages'] as const,
}

export function useChatHistory(
  codebaseId: string,
  sessionId: string | null | undefined,
) {
  return useQuery<ApiResponse<ChatHistoryResponse>>({
    queryKey: chatQueryKeys.messages(codebaseId, sessionId ?? ''),
    queryFn: () => getChatMessagesApi(codebaseId, sessionId!, { limit: 100 }),
    enabled: Boolean(codebaseId) && Boolean(sessionId),
  })
}

export function useChatSessions(codebaseId: string) {
  return useQuery<ApiResponse<ChatSessionResponse[]>>({
    queryKey: chatQueryKeys.sessions(codebaseId),
    queryFn: () => getChatSessionsApi(codebaseId),
    enabled: Boolean(codebaseId),
  })
}

export function useUpdateChatSession(codebaseId: string) {
  const queryClient = useQueryClient()

  return useMutation<
    ApiResponse<ChatSessionResponse>,
    Error,
    { sessionId: string; data: ChatSessionUpdateRequest }
  >({
    mutationFn: ({ sessionId, data }) =>
      updateChatSessionApi(codebaseId, sessionId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: chatQueryKeys.sessions(codebaseId),
      })
    },
  })
}

export function useDeleteChatSession(codebaseId: string) {
  const queryClient = useQueryClient()

  return useMutation<void, Error, string>({
    mutationFn: (sessionId: string) =>
      deleteChatSessionApi(codebaseId, sessionId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: chatQueryKeys.sessions(codebaseId),
      })
    },
  })
}

export function useChatStream(codebaseId: string) {
  const queryClient = useQueryClient()
  const [messages, setMessages] = useState('')
  const [citations, setCitations] = useState<CodeCitation[]>([])
  const [streamTitle, setStreamTitle] = useState<string | null>(null)
  const [isStreaming, setIsStreaming] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [chatId, setChatId] = useState<string | null>(null)
  const abortRef = useRef<AbortController | null>(null)

  function abort() {
    abortRef.current?.abort()
    abortRef.current = null
    setIsStreaming(false)
  }

  function sendMessage(data: CodeChatRequest) {
    // Cancel any in-flight stream
    abort()

    // Reset state for new message
    setMessages('')
    setCitations([])
    setStreamTitle(null)
    setError(null)
    setChatId(null)
    setIsStreaming(true)

    const ctrl = streamChatApi(codebaseId, data, {
      onMessage: (chunk) => {
        setMessages((prev) => {
          if (!prev) return chunk
          if (!chunk) return prev

          // Case 1: Server emitted cumulative content (chunk contains full response so far)
          if (chunk.startsWith(prev)) {
            return chunk
          }

          // Case 2: Cumulative content with minor leading whitespace differences
          if (
            chunk.length > prev.length &&
            chunk.trimStart().startsWith(prev.trimStart())
          ) {
            return chunk
          }

          // Case 3: Server emitted delta token chunk
          return prev + chunk
        })
      },
      onCitations: (newCitations) => {
        setCitations(newCitations)
      },
      onTitle: (title) => {
        setStreamTitle(title)
      },
      onDone: (resolvedChatId) => {
        setChatId(resolvedChatId)
        setIsStreaming(false)
        abortRef.current = null

        // Invalidate sessions list query so new/updated session title shows up
        if (codebaseId) {
          queryClient.invalidateQueries({
            queryKey: chatQueryKeys.sessions(codebaseId),
          })
        }
      },
      onError: (message) => {
        setError(message)
        setIsStreaming(false)
        abortRef.current = null
      },
    })

    abortRef.current = ctrl
  }

  return {
    messages,
    citations,
    streamTitle,
    isStreaming,
    error,
    chatId,
    sendMessage,
    abort,
  }
}
