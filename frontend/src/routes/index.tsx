import { useState } from 'react'
import { createFileRoute, Link, useNavigate } from '@tanstack/react-router'
import { useCurrentUser, useLogout } from '#/features/auth'
import { LogOut, User as UserIcon, Shield, Sparkles, ArrowRight } from 'lucide-react'
import { toast } from 'sonner'

export const Route = createFileRoute('/')({ component: Home })

function Home() {
  const { data: userResponse, isLoading } = useCurrentUser()
  const logoutMutation = useLogout()
  const navigate = useNavigate()
  const [avatarError, setAvatarError] = useState(false)

  const currentUser = userResponse?.data

  const handleLogout = () => {
    logoutMutation.mutate(undefined, {
      onSuccess: () => {
        toast.success('Signed out successfully.')
        navigate({ to: '/login' })
      },
      onError: (err: any) => {
        toast.error(err?.message || 'Logout failed.')
      },
    })
  }

  return (
    <div className="min-h-screen bg-[#080B11] text-slate-100 p-6 sm:p-12 font-sans relative overflow-hidden">
      {/* Dynamic Background Glows */}
      <div className="absolute top-0 left-0 w-full h-full pointer-events-none z-0">
        <div className="absolute top-[-10%] right-[-10%] w-[50%] h-[50%] bg-orange-500/10 blur-[130px] rounded-full" />
        <div className="absolute bottom-[-10%] left-[-10%] w-[50%] h-[50%] bg-cyan-500/10 blur-[130px] rounded-full" />
      </div>

      <div className="max-w-4xl mx-auto relative z-10 space-y-8">
        {/* Navigation Header */}
        <header className="flex items-center justify-between py-4 border-b border-slate-800/80">
          <div className="flex items-center gap-3">
            <img src="/logo.png" alt="CodeCompass Logo" className="w-10 h-10 object-contain shrink-0" />
            <div>
              <h1 className="font-extrabold text-xl text-white tracking-tight flex items-center gap-2">
                CodeCompass
                <span className="inline-block w-2 h-2 rounded-full bg-orange-500 animate-pulse" />
              </h1>
              <p className="text-xs text-slate-400 font-medium">AI Code Intelligence System</p>
            </div>
          </div>

          <div>
            {isLoading ? (
              <div className="w-24 h-9 rounded-xl bg-slate-900 animate-pulse" />
            ) : currentUser ? (
              <button
                onClick={handleLogout}
                disabled={logoutMutation.isPending}
                className="py-2 px-4 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-300 text-xs font-semibold flex items-center gap-2 transition-all cursor-pointer disabled:opacity-50"
              >
                <LogOut className="w-3.5 h-3.5" />
                <span>{logoutMutation.isPending ? 'Signing out...' : 'Sign Out'}</span>
              </button>
            ) : (
              <div className="flex items-center gap-3">
                <Link
                  to="/login"
                  className="py-2 px-4 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-200 text-xs font-semibold transition-all"
                >
                  Sign In
                </Link>
                <Link
                  to="/register"
                  className="py-2 px-4 rounded-xl bg-orange-600 hover:bg-orange-500 text-white text-xs font-semibold shadow-md shadow-orange-950/30 transition-all"
                >
                  Get Started
                </Link>
              </div>
            )}
          </div>
        </header>

        {/* Main Content Area */}
        {isLoading ? (
          <div className="p-12 rounded-3xl bg-slate-950/60 border border-slate-800/80 flex flex-col items-center justify-center space-y-4">
            <div className="w-8 h-8 border-2 border-orange-500 border-t-transparent rounded-full animate-spin" />
            <p className="text-sm text-slate-400">Verifying session...</p>
          </div>
        ) : currentUser ? (
          <div className="p-8 sm:p-10 rounded-3xl bg-[#0F141E]/90 backdrop-blur-2xl border border-slate-800/80 shadow-2xl relative overflow-hidden space-y-6">
            <div className="absolute top-0 left-0 right-0 h-[2px] bg-gradient-to-r from-transparent via-orange-500/50 to-transparent" />

            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pb-6 border-b border-slate-800">
              <div className="flex items-center gap-4">
                {currentUser.avatarUrl && !avatarError ? (
                  <img
                    src={currentUser.avatarUrl}
                    alt={currentUser.fullName}
                    onError={() => setAvatarError(true)}
                    className="w-14 h-14 rounded-2xl object-cover border border-orange-500/30 shadow-lg shadow-orange-950/40 shrink-0"
                  />
                ) : (
                  <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-orange-500 to-amber-600 flex items-center justify-center text-white text-xl font-bold shadow-lg shadow-orange-950/40 shrink-0">
                    {currentUser.fullName ? currentUser.fullName.charAt(0).toUpperCase() : 'U'}
                  </div>
                )}
                <div>
                  <div className="flex items-center gap-2">
                    <h2 className="text-2xl font-extrabold text-white tracking-tight">
                      Welcome back, {currentUser.fullName}!
                    </h2>
                    <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider bg-orange-500/10 text-orange-400 border border-orange-500/20">
                      {currentUser.role}
                    </span>
                  </div>
                  <p className="text-xs text-slate-400 mt-1">
                    @{currentUser.username} &bull; {currentUser.email}
                  </p>
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-2">
              <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-2">
                <div className="w-8 h-8 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 flex items-center justify-center">
                  <Shield className="w-4 h-4" />
                </div>
                <h3 className="text-sm font-semibold text-white">Session Status</h3>
                <p className="text-xs text-emerald-400 font-medium">Authenticated & Token Active</p>
              </div>

              <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-2">
                <div className="w-8 h-8 rounded-lg bg-cyan-500/10 border border-cyan-500/20 text-cyan-400 flex items-center justify-center">
                  <UserIcon className="w-4 h-4" />
                </div>
                <h3 className="text-sm font-semibold text-white">User ID</h3>
                <p className="text-xs text-slate-400 font-mono truncate">{currentUser.id}</p>
              </div>

              <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-2">
                <div className="w-8 h-8 rounded-lg bg-orange-500/10 border border-orange-500/20 text-orange-400 flex items-center justify-center">
                  <Sparkles className="w-4 h-4" />
                </div>
                <h3 className="text-sm font-semibold text-white">Code Intelligence</h3>
                <p className="text-xs text-slate-400">Workspace indexing ready</p>
              </div>
            </div>
          </div>
        ) : (
          <div className="p-10 sm:p-14 rounded-3xl bg-[#0F141E]/90 backdrop-blur-2xl border border-slate-800/80 shadow-2xl text-center space-y-6 relative overflow-hidden">
            <div className="absolute top-0 left-0 right-0 h-[2px] bg-gradient-to-r from-transparent via-orange-500/50 to-transparent" />

            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-orange-500/10 border border-orange-500/20 text-orange-400 text-xs font-semibold">
              <Sparkles className="w-3.5 h-3.5" />
              <span>Next-Gen AI Code Intelligence</span>
            </div>

            <h2 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight max-w-xl mx-auto leading-tight">
              Understand Any Codebase in Seconds
            </h2>

            <p className="text-slate-400 text-sm sm:text-base max-w-lg mx-auto leading-relaxed">
              Sign in or create an account to start performing semantic code searches, mapping dependency graphs, and generating real-time architecture insights.
            </p>

            <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-4">
              <Link
                to="/register"
                className="w-full sm:w-auto py-3 px-6 rounded-xl bg-orange-600 hover:bg-orange-500 border border-orange-500/20 text-white font-semibold text-sm shadow-lg shadow-orange-950/40 flex items-center justify-center gap-2 transition-all"
              >
                <span>Create Free Account</span>
                <ArrowRight className="w-4 h-4" />
              </Link>
              <Link
                to="/login"
                className="w-full sm:w-auto py-3 px-6 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-200 font-semibold text-sm transition-all"
              >
                Sign In to Workspace
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

