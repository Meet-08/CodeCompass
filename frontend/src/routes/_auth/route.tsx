import { createFileRoute, Outlet } from '@tanstack/react-router'
import { Search, BookOpen, GitFork, Cpu, CheckCircle2 } from 'lucide-react'

export const Route = createFileRoute('/_auth')({
  component: AuthLayout,
})

function AuthLayout() {
  return (
    <div className="min-h-screen w-full bg-[#080B11] text-slate-100 flex flex-col lg:flex-row relative overflow-hidden font-sans selection:bg-orange-500/30 selection:text-orange-200">
      {/* Dynamic Background Glow Effects */}
      <div className="absolute top-0 left-0 w-full h-full pointer-events-none overflow-hidden z-0">
        {/* Top-left Amber/Orange Radial Glow */}
        <div className="absolute -top-[20%] -left-[10%] w-[60%] h-[60%] rounded-full bg-gradient-to-br from-orange-500/15 via-amber-500/10 to-transparent blur-[120px]" />
        {/* Bottom-right Deep Indigo/Cyan Glow */}
        <div className="absolute -bottom-[20%] -right-[10%] w-[60%] h-[60%] rounded-full bg-gradient-to-tl from-cyan-500/10 via-indigo-500/10 to-transparent blur-[120px]" />
        {/* Subtle Tech Grid lines overlay */}
        <div className="absolute inset-0 bg-[linear-gradient(to_right,#1e293b15_1px,transparent_1px),linear-gradient(to_bottom,#1e293b15_1px,transparent_1px)] bg-[size:3rem_3rem] [mask-image:radial-gradient(ellipse_60%_50%_at_50%_50%,#000_70%,transparent_100%)]" />
      </div>

      {/* LEFT COLUMN - Brand & Feature Showcase (Visible on lg screens) */}
      <div className="hidden lg:flex lg:w-1/2 flex-col justify-between p-12 xl:p-16 relative z-10 border-r border-slate-800/50 bg-slate-950/20 backdrop-blur-xs">
        {/* Top Logo Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <img
              src="/logo.png"
              alt="CodeCompass Logo"
              className="w-9 h-9 object-contain shrink-0"
            />
            <div className="flex flex-col">
              <span className="font-bold text-xl tracking-tight text-white flex items-center gap-1.5">
                CodeCompass
                <span className="inline-block w-2 h-2 rounded-full bg-orange-500 animate-pulse" />
              </span>
              <span className="text-xs text-slate-400 font-medium tracking-wide">
                AI Code Intelligence
              </span>
            </div>
          </div>
        </div>

        {/* Middle Hero & Feature Highlights */}
        <div className="my-auto py-8 max-w-xl">
          <h1 className="text-4xl xl:text-5xl font-extrabold tracking-tight text-white leading-[1.15] mb-4">
            Navigate Any Codebase with{' '}
            <span className="text-orange-500">AI Precision</span>
          </h1>

          <p className="text-slate-400 text-base xl:text-lg leading-relaxed mb-8">
            CodeCompass empowers engineering teams to understand, search,
            document, and map complex software architectures in seconds using
            advanced semantic AI.
          </p>

          {/* 4 Feature Cards Grid */}
          <div className="grid grid-cols-2 gap-4 mb-8">
            <div className="p-4 rounded-2xl bg-slate-900/50 border border-slate-800/80 hover:border-orange-500/40 hover:bg-slate-900/80 transition-all duration-300 group">
              <div className="w-9 h-9 rounded-xl bg-orange-500/10 border border-orange-500/20 flex items-center justify-center text-orange-400 mb-3 group-hover:scale-110 transition-transform">
                <Search className="w-4 h-4" />
              </div>
              <h3 className="text-sm font-semibold text-white mb-1">
                Semantic Search
              </h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                Search by intent and logic across millions of lines of code.
              </p>
            </div>

            <div className="p-4 rounded-2xl bg-slate-900/50 border border-slate-800/80 hover:border-cyan-500/40 hover:bg-slate-900/80 transition-all duration-300 group">
              <div className="w-9 h-9 rounded-xl bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center text-cyan-400 mb-3 group-hover:scale-110 transition-transform">
                <BookOpen className="w-4 h-4" />
              </div>
              <h3 className="text-sm font-semibold text-white mb-1">
                AI Documentation
              </h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                Auto-generate comprehensive docs and system flow explanations.
              </p>
            </div>

            <div className="p-4 rounded-2xl bg-slate-900/50 border border-slate-800/80 hover:border-purple-500/40 hover:bg-slate-900/80 transition-all duration-300 group">
              <div className="w-9 h-9 rounded-xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400 mb-3 group-hover:scale-110 transition-transform">
                <GitFork className="w-4 h-4" />
              </div>
              <h3 className="text-sm font-semibold text-white mb-1">
                Dependency Graphs
              </h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                Visualize clear module relationships and data dependencies.
              </p>
            </div>

            <div className="p-4 rounded-2xl bg-slate-900/50 border border-slate-800/80 hover:border-emerald-500/40 hover:bg-slate-900/80 transition-all duration-300 group">
              <div className="w-9 h-9 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400 mb-3 group-hover:scale-110 transition-transform">
                <Cpu className="w-4 h-4" />
              </div>
              <h3 className="text-sm font-semibold text-white mb-1">
                Architecture Maps
              </h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                Deep-dive into component structures and execution paths.
              </p>
            </div>
          </div>

          {/* Interactive Code Preview Graphic */}
          <div className="rounded-2xl bg-slate-900/90 border border-slate-800 shadow-2xl p-4 font-mono text-xs overflow-hidden backdrop-blur-md relative">
            <div className="flex items-center justify-between border-b border-slate-800/80 pb-3 mb-3">
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-rose-500/80 inline-block" />
                <span className="w-2.5 h-2.5 rounded-full bg-amber-500/80 inline-block" />
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-500/80 inline-block" />
                <span className="ml-2 text-slate-400 text-[11px] font-sans">
                  UserService.ts
                </span>
              </div>
              <div className="flex items-center gap-1.5 px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 text-[10px] font-medium border border-emerald-500/20">
                <CheckCircle2 className="w-3 h-3" /> Index Synced
              </div>
            </div>

            <div className="space-y-1 text-slate-300">
              <div className="text-slate-500">
                // AI Query: Map authentication flow and token refresh
              </div>
              <div>
                <span className="text-purple-400">export async function</span>{' '}
                <span className="text-blue-400">authenticateUser</span>
                (credentials: UserAuth) &#123;
              </div>
              <div className="pl-4 text-slate-400">
                // Querying CodeCompass Vector Graph...
              </div>
              <div className="pl-4 bg-orange-500/10 text-orange-200 border-l-2 border-orange-500 py-0.5 px-2 rounded-r">
                <span className="text-purple-400">const</span> token ={' '}
                <span className="text-purple-400">await</span>{' '}
                AuthEngine.verifyAndSign(credentials);
              </div>
              <div className="pl-4">
                <span className="text-purple-400">return</span> &#123; status:{' '}
                <span className="text-emerald-400">'AUTHENTICATED'</span>, token
                &#125;;
              </div>
              <div>&#125;</div>
            </div>
          </div>
        </div>

        {/* Footer info */}
        <div className="flex items-center justify-between text-xs text-slate-500 pt-4 border-t border-slate-800/40">
          <span>&copy; {new Date().getFullYear()} CodeCompass AI Inc.</span>
          <div className="flex items-center gap-4">
            <a href="#" className="hover:text-slate-400 transition-colors">
              Privacy
            </a>
            <a href="#" className="hover:text-slate-400 transition-colors">
              Terms
            </a>
            <a href="#" className="hover:text-slate-400 transition-colors">
              Security
            </a>
          </div>
        </div>
      </div>

      {/* RIGHT COLUMN - Form Outlet Container (Login / Register) */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-6 sm:p-10 lg:p-12 xl:p-16 relative z-10 my-auto">
        {/* Mobile Header (Shown only on small screens) */}
        <div className="absolute top-6 left-6 lg:hidden flex items-center gap-2.5">
          <img
            src="/logo.png"
            alt="CodeCompass Logo"
            className="w-8 h-8 object-contain"
          />
          <span className="font-bold text-lg text-white">CodeCompass</span>
        </div>

        <div className="w-full max-w-md">
          <Outlet />
        </div>
      </div>
    </div>
  )
}
