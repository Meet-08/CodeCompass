import { useRef, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  getCodebasesApi,
  importCodebaseApi,
  updateCodebaseApi,
  deleteCodebaseApi,
  reindexCodebaseApi,
  streamChatApi,
} from '../api/codebase.api'
import type { ApiResponse } from '#/types/api.types'
import type {
  CodebaseResponse,
  CodebaseImportRequest,
  CodebaseImportResponse,
  CodebaseUpdateRequest,
  CodeChatRequest,
  CodeCitation,
} from '../types/codebase.types'

export const codebaseQueryKeys = {
  all: ['codebases'] as const,
  detail: (id: string) => [...codebaseQueryKeys.all, id] as const,
  chat: (id: string, chatId: string) =>
    [...codebaseQueryKeys.detail(id), 'chat', chatId] as const,
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
    mutationFn: ({ codebaseId, data }) =>
      updateCodebaseApi(codebaseId, data),
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

export function useChatStream(codebaseId: string) {
  const [messages, setMessages] = useState('')
  const [citations, setCitations] = useState<CodeCitation[]>([])
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
      onDone: (resolvedChatId) => {
        setChatId(resolvedChatId)
        setIsStreaming(false)
        abortRef.current = null
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
    isStreaming,
    error,
    chatId,
    sendMessage,
    abort,
  }
}
