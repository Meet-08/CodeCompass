import type { z } from 'zod'
import type {
  codebaseStatusSchema,
  codebaseResponseSchema,
  codebaseImportRequestSchema,
  codebaseImportResponseSchema,
  codebaseUpdateRequestSchema,
  codeChatRequestSchema,
  codeCitationSchema,
} from '../schemas/codebase.schema'

export type CodebaseStatus = z.infer<typeof codebaseStatusSchema>
export type CodebaseResponse = z.infer<typeof codebaseResponseSchema>
export type CodebaseImportRequest = z.infer<typeof codebaseImportRequestSchema>
export type CodebaseImportResponse = z.infer<
  typeof codebaseImportResponseSchema
>
export type CodebaseUpdateRequest = z.infer<typeof codebaseUpdateRequestSchema>
export type CodeChatRequest = z.infer<typeof codeChatRequestSchema>
export type CodeCitation = z.infer<typeof codeCitationSchema>

export type ChatStreamEvent =
  | { event: 'message'; data: string }
  | { event: 'citations'; data: CodeCitation[] }
  | { event: 'done'; data: string }
  | { event: 'error'; data: { message: string } }
