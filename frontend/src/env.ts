import { createEnv } from '@t3-oss/env-core'
import { z } from 'zod'

export const env = createEnv({
  clientPrefix: 'VITE_',
  client: {
    VITE_APP_TITLE: z.string().min(1).optional(),
    VITE_API_BASE_URL: z.url().optional().default('http://localhost:8080'),
  },
  runtimeEnv: import.meta.env,
  emptyStringAsUndefined: true,
})
