export interface ApiResponse<T = unknown> {
  success: boolean
  message: string
  data: T | null
}

export interface ApiValidationErrorResponse {
  success: false
  message: string
  data: Record<string, string>
}

export interface ApiErrorResponse {
  success: false
  message: string
  data?: null | Record<string, string>
}
