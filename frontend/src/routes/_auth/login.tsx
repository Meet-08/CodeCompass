import { useState } from 'react'
import { createFileRoute, Link, useNavigate } from '@tanstack/react-router'
import { Mail, Lock, Eye, EyeOff, ArrowRight } from 'lucide-react'
import { toast } from 'sonner'
import { env } from '#/env'
import { useLogin } from '#/features/auth'
import { getApiErrorMessage } from '#/lib/utils'

export const Route = createFileRoute('/_auth/login')({
  component: LoginComponent,
})

function LoginComponent() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)

  const loginMutation = useLogin()
  const navigate = useNavigate()

  const handleSubmit = (e: React.SubmitEvent) => {
    e.preventDefault()

    loginMutation.mutate(
      { email, password },
      {
        onSuccess: (res) => {
          if (res.success) {
            const msg =
              res.message || 'Authentication successful! Redirecting...'
            toast.success(msg)
            setTimeout(() => {
              navigate({ to: '/' })
            }, 800)
          } else {
            const msg = res.message || 'Login failed'
            toast.error(msg)
          }
        },
        onError: (err: unknown) => {
          const message = getApiErrorMessage(
            err,
            'Failed to sign in. Please check your credentials.',
          )
          toast.error(message)
        },
      },
    )
  }

  const handleOAuthClick = (provider: 'github' | 'google') => {
    const baseUrl = env.VITE_API_BASE_URL.replace(/\/+$/, '')
    window.location.href = `${baseUrl}/oauth2/authorization/${provider}`
  }

  const handleForgotPassword = (e: React.MouseEvent) => {
    e.preventDefault()
    if (!email) {
      toast.error('Please enter your work email address first.')
      return
    }
    toast.success(`Password reset link sent to ${email}!`)
  }

  return (
    <div className="w-full bg-[#0F141E]/90 backdrop-blur-2xl border border-slate-800/80 rounded-3xl p-7 sm:p-9 shadow-[0_0_50px_rgba(0,0,0,0.5)] relative overflow-hidden group">
      {/* Top subtle inner glow line */}
      <div className="absolute top-0 left-0 right-0 h-[2px] bg-gradient-to-r from-transparent via-orange-500/40 to-transparent" />

      {/* Card Header */}
      <div className="mb-7 text-left">
        <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight mb-2">
          Sign in to CodeCompass
        </h2>
        <p className="text-slate-400 text-sm">
          Enter your email and password to access your codebase intelligence
          workspace.
        </p>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Email Field */}
        <div>
          <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
            Work Email Address
          </label>
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
              <Mail className="w-4 h-4" />
            </div>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="developer@company.com"
              className="w-full pl-10 pr-4 py-2.5 bg-slate-950/60 border border-slate-800 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-500/20 transition-all"
            />
          </div>
        </div>

        {/* Password Field */}
        <div>
          <div className="flex items-center justify-between mb-1.5">
            <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider">
              Password
            </label>
            <a
              href="#"
              onClick={handleForgotPassword}
              className="text-xs font-medium text-orange-400 hover:text-orange-300 transition-colors"
            >
              Forgot password?
            </a>
          </div>
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
              <Lock className="w-4 h-4" />
            </div>
            <input
              type={showPassword ? 'text' : 'password'}
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••••••"
              className="w-full pl-10 pr-10 py-2.5 bg-slate-950/60 border border-slate-800 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-500/20 transition-all"
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-slate-200 transition-colors cursor-pointer"
            >
              {showPassword ? (
                <EyeOff className="w-4 h-4" />
              ) : (
                <Eye className="w-4 h-4" />
              )}
            </button>
          </div>
        </div>

        {/* Submit Button */}
        <button
          type="submit"
          disabled={loginMutation.isPending}
          className="w-full mt-3 py-3 px-4 rounded-xl bg-orange-600 hover:bg-orange-500 border border-orange-500/20 text-white font-semibold text-sm shadow-md shadow-orange-950/30 active:scale-[0.99] transition-all flex items-center justify-center gap-2 group cursor-pointer disabled:opacity-70"
        >
          {loginMutation.isPending ? (
            <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
          ) : (
            <>
              <span>Sign In</span>
              <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
            </>
          )}
        </button>
      </form>

      {/* Or Divider */}
      <div className="relative my-6">
        <div className="absolute inset-0 flex items-center">
          <div className="w-full border-t border-slate-800" />
        </div>
        <div className="relative flex justify-center text-xs uppercase">
          <span className="bg-[#0F141E] px-3 text-slate-500 font-medium tracking-wider">
            Or continue with
          </span>
        </div>
      </div>

      {/* Social Logins */}
      <div className="grid grid-cols-2 gap-3">
        {/* GitHub */}
        <button
          type="button"
          onClick={() => handleOAuthClick('github')}
          className="py-2.5 px-3 rounded-xl bg-slate-900/80 hover:bg-slate-800/90 border border-slate-800 hover:border-slate-700 text-slate-200 text-xs font-semibold flex items-center justify-center gap-2 transition-all cursor-pointer"
        >
          <svg className="w-4 h-4 fill-current" viewBox="0 0 24 24">
            <path
              fillRule="evenodd"
              clipRule="evenodd"
              d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"
            />
          </svg>
          <span>GitHub</span>
        </button>

        {/* Google */}
        <button
          type="button"
          onClick={() => handleOAuthClick('google')}
          className="py-2.5 px-3 rounded-xl bg-slate-900/80 hover:bg-slate-800/90 border border-slate-800 hover:border-slate-700 text-slate-200 text-xs font-semibold flex items-center justify-center gap-2 transition-all cursor-pointer"
        >
          <svg className="w-4 h-4" viewBox="0 0 24 24">
            <path
              fill="#4285F4"
              d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
            />
            <path
              fill="#34A853"
              d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
            />
            <path
              fill="#FBBC05"
              d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
            />
            <path
              fill="#EA4335"
              d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
            />
          </svg>
          <span>Google</span>
        </button>
      </div>

      {/* Switch to Register */}
      <div className="mt-7 text-center text-xs text-slate-400">
        Don't have an account?{' '}
        <Link
          to="/register"
          className="text-orange-400 font-semibold hover:text-orange-300 transition-colors underline-offset-4 hover:underline"
        >
          Sign up for free
        </Link>
      </div>

      {/* Footer Disclaimer */}
      <div className="mt-5 text-[11px] text-slate-500 text-center leading-relaxed">
        By signing in, you agree to CodeCompass's{' '}
        <a href="#" className="text-slate-400 hover:underline">
          Terms of Service
        </a>{' '}
        and{' '}
        <a href="#" className="text-slate-400 hover:underline">
          Privacy Policy
        </a>
        .
      </div>
    </div>
  )
}
