import { z } from 'zod'
import {
  userRoleSchema,
  userResponseSchema,
  authResponseSchema,
  registerSchema,
  loginSchema,
} from '../schemas/auth.schema'

export type UserRole = z.infer<typeof userRoleSchema>
export type UserResponse = z.infer<typeof userResponseSchema>
export type AuthResponse = z.infer<typeof authResponseSchema>
export type RegisterInput = z.infer<typeof registerSchema>
export type LoginInput = z.infer<typeof loginSchema>
