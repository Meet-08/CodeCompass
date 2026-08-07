import { z } from 'zod'

export const chatSessionResponseSchema = z.object({
  sessionId: z.uuid(),
  codebaseId: z.uuid(),
  title: z.string(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export const chatSessionUpdateRequestSchema = z.object({
  title: z.string().min(1, 'Title is required'),
})

export const codeChatRequestSchema = z.object({
  chatId: z.string().optional(),
  message: z.string().min(1, 'Message is required'),
})

export const codeCitationSchema = z.object({
  chunkId: z.uuid(),
  path: z.string(),
  startLine: z.number().int().nullable(),
  endLine: z.number().int().nullable(),
  language: z.string().nullable(),
  distance: z.number(),
})

export const chatMessageResponseSchema = z.object({
  messageId: z.uuid(),
  role: z.enum(['USER', 'ASSISTANT']),
  content: z.string(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export const chatHistoryResponseSchema = z.object({
  messages: z.array(chatMessageResponseSchema),
  hasMore: z.boolean(),
  nextCursor: z.string().nullable(),
})
