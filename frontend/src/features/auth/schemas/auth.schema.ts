import { z } from 'zod'

export const userRoleSchema = z.enum(['USER', 'ADMIN'])

export const userResponseSchema = z.object({
  id: z.uuid(),
  fullName: z.string(),
  username: z.string(),
  email: z.email(),
  avatarUrl: z.string().nullable().optional(),
  role: userRoleSchema,
})

export const authResponseSchema = z.object({
  accessToken: z.string(),
  user: userResponseSchema,
})

export const registerSchema = z.object({
  fullName: z
    .string()
    .min(1, 'Full name is required')
    .max(100, 'Full name must not exceed 100 characters'),
  username: z
    .string()
    .min(1, 'Username is required')
    .max(50, 'Username must not exceed 50 characters'),
  email: z
    .email()
    .min(1, 'Email is required')
    .max(255, 'Email must not exceed 255 characters'),
  password: z
    .string()
    .min(8, 'Password must be at least 8 characters')
    .max(100, 'Password must not exceed 100 characters'),
})

export const loginSchema = z.object({
  email: z.email().min(1, 'Email is required'),
  password: z.string().min(1, 'Password is required'),
})
