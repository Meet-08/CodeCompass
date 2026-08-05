import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { z } from 'zod'
import { authQueryKeys } from '#/features/auth/hooks/use-auth'
import { tokenManager } from '#/lib/token-manager'

const callbackSearchSchema = z.object({
  access_token: z.string().optional(),
  error: z.string().optional(),
})

export const Route = createFileRoute('/oauth2/callback')({
  validateSearch: (search) => callbackSearchSchema.parse(search),
  component: OAuthCallbackComponent,
})

function OAuthCallbackComponent() {
  const { access_token, error } = Route.useSearch()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  useEffect(() => {
    if (access_token) {
      tokenManager.setAccessToken(access_token)
      queryClient.invalidateQueries({ queryKey: authQueryKeys.all })
      toast.success('Signed in successfully!')
      navigate({ to: '/' })
    } else if (error) {
      toast.error('Sign in failed. Please try again.')
      navigate({ to: '/login' })
    } else {
      navigate({ to: '/login' })
    }
  }, [access_token, error, navigate, queryClient])

  return (
    <div className="min-h-screen bg-[#080B11] text-slate-100 flex flex-col items-center justify-center p-6 font-sans">
      <div className="p-8 rounded-3xl bg-[#0F141E]/90 backdrop-blur-2xl border border-slate-800/80 shadow-2xl flex flex-col items-center space-y-4 max-w-sm text-center">
        <div className="w-9 h-9 border-2 border-orange-500 border-t-transparent rounded-full animate-spin" />
        <h2 className="text-lg font-bold text-white tracking-tight">
          Completing sign in...
        </h2>
        <p className="text-xs text-slate-400 leading-relaxed">
          Please wait while we set up your session and redirect to your
          workspace.
        </p>
      </div>
    </div>
  )
}
