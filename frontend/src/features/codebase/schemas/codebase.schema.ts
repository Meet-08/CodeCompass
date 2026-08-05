import { z } from 'zod'

export const codebaseStatusSchema = z.enum([
  'INDEXED',
  'QUEUED',
  'PROCESSING',
  'FAILED',
])

export const codebaseImportRequestSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  cloneUrl: z
    .string()
    .min(1, 'Clone URL is required')
    .regex(/^https:\/\/.+$/i, 'Clone URL must use HTTPS'),
  branch: z.string().optional(),
})

export const codebaseImportResponseSchema = z.object({
  codebaseId: z.uuid(),
  status: codebaseStatusSchema,
  fileCount: z.number().int().min(0),
})

export const codeChatRequestSchema = z.object({
  chatId: z.string().optional(),
  message: z.string().min(1, 'Message is required'),
})

export const codebaseResponseSchema = z.object({
  codebaseId: z.uuid(),
  name: z.string(),
  cloneUrl: z.string().nullable(),
  branch: z.string().nullable(),
  status: codebaseStatusSchema,
  lastCommitSha: z.string().nullable(),
  indexedAt: z.string().nullable(),
  createdAt: z.string(),
  updatedAt: z.string(),
  fileCount: z.number().int().min(0),
})

export const codebaseUpdateRequestSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  branch: z.string().min(1, 'Branch is required'),
})

export const codeCitationSchema = z.object({
  chunkId: z.uuid(),
  path: z.string(),
  startLine: z.number().int().nullable(),
  endLine: z.number().int().nullable(),
  language: z.string().nullable(),
  distance: z.number(),
})
