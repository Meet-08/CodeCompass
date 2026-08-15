import type { z } from 'zod'
import type {
  chatSessionResponseSchema,
  chatSessionUpdateRequestSchema,
  codeChatRequestSchema,
  codeCitationSchema,
  chatMessageResponseSchema,
  chatHistoryResponseSchema,
} from '../schemas/chat.schema'

export type ChatSessionResponse = z.infer<typeof chatSessionResponseSchema>
export type ChatSessionUpdateRequest = z.infer<
  typeof chatSessionUpdateRequestSchema
>
export type CodeChatRequest = z.infer<typeof codeChatRequestSchema>
export type CodeCitation = z.infer<typeof codeCitationSchema>
export type ChatMessageResponse = z.infer<typeof chatMessageResponseSchema>
export type ChatHistoryResponse = z.infer<typeof chatHistoryResponseSchema>
export type MessageRole = 'USER' | 'ASSISTANT' | 'SYSTEM'

export type ChatStreamEvent =
  | { event: 'message'; data: string }
  | { event: 'citations'; data: CodeCitation[] }
  | { event: 'title'; data: string }
  | { event: 'done'; data: string }
  | { event: 'error'; data: { message: string } }
