import type { ClassValue } from 'clsx'
import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function getApiErrorMessage(
  error: unknown,
  fallbackMessage = 'An unexpected error occurred'
): string {
  if (!error || typeof error !== 'object') {
    return fallbackMessage
  }

  const errObj = error as {
    response?: {
      data?: {
        message?: string
        data?: unknown
      }
    }
    message?: string
  }

  const responseData = errObj.response?.data

  if (responseData) {
    if (
      responseData.data &&
      typeof responseData.data === 'object' &&
      !Array.isArray(responseData.data)
    ) {
      const fieldErrors = Object.entries(responseData.data)
        .map(([field, msg]) => `${field}: ${msg}`)
        .join('; ')

      if (fieldErrors) {
        return fieldErrors
      }
    }

    if (responseData.message) {
      return responseData.message
    }
  }

  if (errObj.message) {
    return errObj.message
  }

  return fallbackMessage
}

