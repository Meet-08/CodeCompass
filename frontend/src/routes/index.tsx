import { useState } from 'react'
import { createFileRoute, Link, useNavigate } from '@tanstack/react-router'
import { useCurrentUser, useLogout } from '#/features/auth'
import { useCodebases } from '#/features/codebase'
import {
  LogOut,
  User as UserIcon,
  Sparkles,
  ArrowRight,
  Layers,
  FolderPlus,
  FileCode,
  MessageSquare,
  GitBranch,
  Zap,
  Shield,
  Search,
} from 'lucide-react'
import { toast } from 'sonner'
import { getApiErrorMessage } from '#/lib/utils'

export const Route = createFileRoute('/')({ component: Home })

function Home() {
  const { data: userResponse, isLoading } = useCurrentUser()
  const logoutMutation = useLogout()
  const navigate = useNavigate()
  const [avatarError, setAvatarError] = useState(false)

  const currentUser = userResponse?.data

  // Fetch codebase stats for authenticated users
  const { data: codebasesResponse } = useCodebases()
  const codebases = codebasesResponse?.data ?? []
  const indexedCount = codebases.filter((c) => c.status === 'INDEXED').length
  const totalFiles = codebases.reduce((acc, c) => acc + (c.fileCount || 0), 0)

  const handleLogout = () => {
    logoutMutation.mutate(undefined, {
      onSuccess: () => {
        toast.success('Signed out successfully.')
        navigate({ to: '/login' })
      },
      onError: (err: unknown) => {
        toast.error(getApiErrorMessage(err, 'Logout failed.'))
      },
    })
  }

  return (
    <div className="min-h-screen bg-[#080B11] text-slate-100 p-6 sm:p-12 font-sans relative overflow-hidden">
      {/* Dynamic Background Glows */}
      <div className="absolute top-0 left-0 w-full h-full pointer-events-none z-0">
        <div className="absolute top-[-10%] right-[-10%] w-[50%] h-[50%] bg-orange-500/10 blur-[130px] rounded-full" />
        <div className="absolute bottom-[-10%] left-[-10%] w-[50%] h-[50%] bg-cyan-500/10 blur-[130px] rounded-full" />
        <div className="absolute top-[40%] left-[30%] w-[30%] h-[30%] bg-violet-500/5 blur-[100px] rounded-full" />
      </div>

      <div className="max-w-5xl mx-auto relative z-10 space-y-8">
        {/* Navigation Header */}
        <header className="flex items-center justify-between py-4 border-b border-slate-800/80">
          <div className="flex items-center gap-3">
            <img
              src="/logo.png"
              alt="CodeCompass Logo"
              className="w-10 h-10 object-contain shrink-0"
            />
            <div>
              <h1 className="font-extrabold text-xl text-white tracking-tight flex items-center gap-2">
                CodeCompass
                <span className="inline-block w-2 h-2 rounded-full bg-orange-500 animate-pulse" />
              </h1>
              <p className="text-xs text-slate-400 font-medium">
                AI Code Intelligence System
              </p>
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
                <span>
                  {logoutMutation.isPending ? 'Signing out...' : 'Sign Out'}
                </span>
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
          <div className="space-y-6">
            {/* Welcome Card */}
            <div className="p-8 sm:p-10 rounded-3xl bg-[#0F141E]/90 backdrop-blur-2xl border border-slate-800/80 shadow-2xl relative overflow-hidden">
              <div className="absolute top-0 left-0 right-0 h-[2px] bg-gradient-to-r from-transparent via-orange-500/50 to-transparent" />

              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
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
                      {currentUser.fullName
                        ? currentUser.fullName.charAt(0).toUpperCase()
                        : 'U'}
                    </div>
                  )}
                  <div>
                    <div className="flex items-center gap-2 flex-wrap">
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
            </div>

            {/* Quick Stats Row */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="p-5 rounded-3xl bg-[#0F141E]/90 border border-slate-800/80 space-y-2 relative overflow-hidden">
                <div className="absolute top-0 left-0 right-0 h-[1px] bg-gradient-to-r from-transparent via-orange-500/30 to-transparent" />
                <div className="w-8 h-8 rounded-xl bg-orange-500/10 border border-orange-500/20 text-orange-400 flex items-center justify-center">
                  <Layers className="w-4 h-4" />
                </div>
                <h3 className="text-xs font-semibold text-slate-400">
                  Your Codebases
                </h3>
                <p className="text-2xl font-extrabold text-white tracking-tight">
                  {codebases.length}
                  <span className="text-xs font-normal text-slate-500 ml-1">
                    / 5 max
                  </span>
                </p>
              </div>

              <div className="p-5 rounded-3xl bg-[#0F141E]/90 border border-slate-800/80 space-y-2 relative overflow-hidden">
                <div className="absolute top-0 left-0 right-0 h-[1px] bg-gradient-to-r from-transparent via-emerald-500/30 to-transparent" />
                <div className="w-8 h-8 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 flex items-center justify-center">
                  <Sparkles className="w-4 h-4" />
                </div>
                <h3 className="text-xs font-semibold text-slate-400">
                  Indexed & Ready
                </h3>
                <p className="text-2xl font-extrabold text-emerald-400 tracking-tight">
                  {indexedCount}
                  <span className="text-xs font-normal text-slate-500 ml-1">
                    / {codebases.length}
                  </span>
                </p>
              </div>

              <div className="p-5 rounded-3xl bg-[#0F141E]/90 border border-slate-800/80 space-y-2 relative overflow-hidden">
                <div className="absolute top-0 left-0 right-0 h-[1px] bg-gradient-to-r from-transparent via-cyan-500/30 to-transparent" />
                <div className="w-8 h-8 rounded-xl bg-cyan-500/10 border border-cyan-500/20 text-cyan-400 flex items-center justify-center">
                  <FileCode className="w-4 h-4" />
                </div>
                <h3 className="text-xs font-semibold text-slate-400">
                  Indexed Files
                </h3>
                <p className="text-2xl font-extrabold text-cyan-400 tracking-tight">
                  {totalFiles.toLocaleString()}
                </p>
              </div>
            </div>

            {/* Primary Actions */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Link
                to="/codebases"
                className="group p-6 rounded-3xl bg-gradient-to-br from-orange-600/90 to-orange-700/90 hover:from-orange-500/90 hover:to-orange-600/90 border border-orange-500/20 shadow-xl shadow-orange-950/30 transition-all relative overflow-hidden"
              >
                <div className="absolute top-0 right-0 w-[50%] h-[80%] bg-white/5 blur-[60px] rounded-full pointer-events-none" />
                <div className="relative space-y-3">
                  <div className="w-10 h-10 rounded-2xl bg-white/10 border border-white/10 flex items-center justify-center">
                    <Layers className="w-5 h-5 text-white" />
                  </div>
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="text-base font-extrabold text-white tracking-tight">
                        Codebases Dashboard
                      </h3>
                      <p className="text-xs text-orange-200/80 mt-0.5">
                        Manage repositories, view indexing status, and start AI chats
                      </p>
                    </div>
                    <ArrowRight className="w-5 h-5 text-white/70 group-hover:translate-x-1 group-hover:text-white transition-all shrink-0" />
                  </div>
                </div>
              </Link>

              <Link
                to="/codebases"
                className="group p-6 rounded-3xl bg-[#0F141E]/90 hover:bg-[#141A26] border border-slate-800/80 hover:border-slate-700/80 shadow-xl transition-all relative overflow-hidden"
              >
                <div className="absolute top-0 left-0 right-0 h-[1px] bg-gradient-to-r from-transparent via-cyan-500/30 to-transparent" />
                <div className="space-y-3">
                  <div className="w-10 h-10 rounded-2xl bg-cyan-500/10 border border-cyan-500/20 text-cyan-400 flex items-center justify-center">
                    <FolderPlus className="w-5 h-5" />
                  </div>
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="text-base font-extrabold text-white tracking-tight group-hover:text-cyan-400 transition-colors">
                        Import Repository
                      </h3>
                      <p className="text-xs text-slate-400 mt-0.5">
                        Add a new HTTPS Git repository for AI-powered code analysis
                      </p>
                    </div>
                    <ArrowRight className="w-5 h-5 text-slate-500 group-hover:text-cyan-400 group-hover:translate-x-1 transition-all shrink-0" />
                  </div>
                </div>
              </Link>
            </div>

            {/* Feature Highlights */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="p-5 rounded-2xl bg-[#0F141E]/60 border border-slate-800/60 space-y-2.5">
                <div className="w-8 h-8 rounded-lg bg-violet-500/10 border border-violet-500/20 text-violet-400 flex items-center justify-center">
                  <MessageSquare className="w-4 h-4" />
                </div>
                <h3 className="text-sm font-bold text-white">
                  AI Code Chat
                </h3>
                <p className="text-[11px] text-slate-400 leading-relaxed">
                  Ask questions about any indexed codebase and get context-aware
                  answers with source citations.
                </p>
              </div>

              <div className="p-5 rounded-2xl bg-[#0F141E]/60 border border-slate-800/60 space-y-2.5">
                <div className="w-8 h-8 rounded-lg bg-amber-500/10 border border-amber-500/20 text-amber-400 flex items-center justify-center">
                  <Search className="w-4 h-4" />
                </div>
                <h3 className="text-sm font-bold text-white">
                  Semantic Search
                </h3>
                <p className="text-[11px] text-slate-400 leading-relaxed">
                  Intelligent similarity-based code retrieval across your entire
                  indexed repository.
                </p>
              </div>

              <div className="p-5 rounded-2xl bg-[#0F141E]/60 border border-slate-800/60 space-y-2.5">
                <div className="w-8 h-8 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 flex items-center justify-center">
                  <GitBranch className="w-4 h-4" />
                </div>
                <h3 className="text-sm font-bold text-white">
                  Multi-Branch Support
                </h3>
                <p className="text-[11px] text-slate-400 leading-relaxed">
                  Index any branch of your repository and reindex when code
                  changes.
                </p>
              </div>
            </div>
          </div>
        ) : (
          /* Unauthenticated Hero */
          <div className="space-y-8">
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
                Sign in or create an account to start performing semantic code
                searches, mapping dependency graphs, and generating real-time
                architecture insights.
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

            {/* Feature Cards for unauthenticated */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="p-6 rounded-3xl bg-[#0F141E]/70 border border-slate-800/60 space-y-3 relative overflow-hidden">
                <div className="absolute top-0 left-0 right-0 h-[1px] bg-gradient-to-r from-transparent via-orange-500/20 to-transparent" />
                <div className="w-10 h-10 rounded-2xl bg-orange-500/10 border border-orange-500/20 text-orange-400 flex items-center justify-center">
                  <Zap className="w-5 h-5" />
                </div>
                <h3 className="text-sm font-bold text-white">
                  Instant Indexing
                </h3>
                <p className="text-xs text-slate-400 leading-relaxed">
                  Import any public HTTPS repository and have it automatically
                  cloned, parsed, and indexed for AI queries.
                </p>
              </div>

              <div className="p-6 rounded-3xl bg-[#0F141E]/70 border border-slate-800/60 space-y-3 relative overflow-hidden">
                <div className="absolute top-0 left-0 right-0 h-[1px] bg-gradient-to-r from-transparent via-violet-500/20 to-transparent" />
                <div className="w-10 h-10 rounded-2xl bg-violet-500/10 border border-violet-500/20 text-violet-400 flex items-center justify-center">
                  <MessageSquare className="w-5 h-5" />
                </div>
                <h3 className="text-sm font-bold text-white">
                  Contextual Chat
                </h3>
                <p className="text-xs text-slate-400 leading-relaxed">
                  Ask natural language questions and receive answers with direct
                  source code citations and line references.
                </p>
              </div>

              <div className="p-6 rounded-3xl bg-[#0F141E]/70 border border-slate-800/60 space-y-3 relative overflow-hidden">
                <div className="absolute top-0 left-0 right-0 h-[1px] bg-gradient-to-r from-transparent via-emerald-500/20 to-transparent" />
                <div className="w-10 h-10 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 flex items-center justify-center">
                  <Shield className="w-5 h-5" />
                </div>
                <h3 className="text-sm font-bold text-white">
                  Secure & Private
                </h3>
                <p className="text-xs text-slate-400 leading-relaxed">
                  JWT-authenticated access with per-user isolation. Your
                  codebases and conversations are private to you.
                </p>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
