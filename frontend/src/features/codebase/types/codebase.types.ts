import type { z } from 'zod'
import type {
  codebaseStatusSchema,
  codebaseResponseSchema,
  codebaseImportRequestSchema,
  codebaseImportResponseSchema,
  codebaseUpdateRequestSchema,
} from '../schemas/codebase.schema'

export type CodebaseStatus = z.infer<typeof codebaseStatusSchema>
export type CodebaseResponse = z.infer<typeof codebaseResponseSchema>
export type CodebaseImportRequest = z.infer<typeof codebaseImportRequestSchema>
export type CodebaseImportResponse = z.infer<
  typeof codebaseImportResponseSchema
>
export type CodebaseUpdateRequest = z.infer<typeof codebaseUpdateRequestSchema>
