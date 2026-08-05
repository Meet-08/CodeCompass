import { createFileRoute, Outlet, redirect } from '@tanstack/react-router'
import { authQueryKeys, getCurrentUserApi, refreshApi } from '#/features/auth'
import { tokenManager } from '#/lib/token-manager'

export const Route = createFileRoute('/_app')({
  beforeLoad: async ({ context }) => {
    // If running on the server (SSR), skip auth fetch since Node server context does not carry browser cookies
    if (typeof window === 'undefined') {
      return
    }

    // Check if the user is already cached in the query client
    const cached = context.queryClient.getQueryData(authQueryKeys.me())

    if (!cached) {
      // If no access token exists, attempt refresh first using HttpOnly cookie
      if (!tokenManager.hasAccessToken()) {
        try {
          const refreshRes = await refreshApi()
          if (refreshRes.success && refreshRes.data?.accessToken) {
            tokenManager.setAccessToken(refreshRes.data.accessToken)
          }
        } catch {
          // Token refresh failed or cookie missing
        }
      }

      try {
        const response = await context.queryClient.fetchQuery({
          queryKey: authQueryKeys.me(),
          queryFn: getCurrentUserApi,
          staleTime: 1000 * 60 * 10,
        })

        if (!response.success || !response.data) {
          throw redirect({ to: '/login' })
        }
      } catch (error) {
        if (error && typeof error === 'object' && 'to' in error) {
          throw error
        }
        throw redirect({ to: '/login' })
      }
    }
  },
  component: AppLayout,
})

function AppLayout() {
  return <Outlet />
}
