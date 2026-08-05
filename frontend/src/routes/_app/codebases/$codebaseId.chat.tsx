import { createFileRoute, Link } from '@tanstack/react-router'
import { ArrowLeft, GitBranch, FolderGit2 } from 'lucide-react'
import { CodebaseChatContainer, useCodebases } from '#/features/codebase'

export const Route = createFileRoute('/_app/codebases/$codebaseId/chat')({
  component: CodebaseChatPage,
})

function CodebaseChatPage() {
  const { codebaseId } = Route.useParams()
  const { data: codebasesResponse } = useCodebases()

  const codebaseInfo = codebasesResponse?.data?.find(
    (c) => c.codebaseId === codebaseId,
  )

  return (
    <div className="min-h-screen bg-[#080B11] text-slate-100 p-4 sm:p-8 font-sans relative overflow-hidden flex flex-col space-y-4">
      {/* Dynamic Background Glow */}
      <div className="absolute top-0 left-0 w-full h-full pointer-events-none z-0">
        <div className="absolute top-[-10%] left-[20%] w-[40%] h-[40%] bg-orange-500/10 blur-[130px] rounded-full" />
      </div>

      <div className="max-w-6xl w-full mx-auto relative z-10 flex-1 flex flex-col space-y-4">
        {/* Navigation Bar */}
        <header className="flex items-center justify-between py-2 px-1">
          <div className="flex items-center gap-3">
            <Link
              to="/codebases"
              className="p-2 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-400 hover:text-slate-200 transition-all cursor-pointer flex items-center gap-1.5 text-xs font-semibold"
            >
              <ArrowLeft className="w-4 h-4" />
              <span className="hidden sm:inline">Back to Codebases</span>
            </Link>

            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-xl bg-slate-900 border border-slate-800 text-orange-400 flex items-center justify-center">
                <FolderGit2 className="w-4 h-4" />
              </div>
              <div>
                <h1 className="text-sm font-extrabold text-white tracking-tight flex items-center gap-2">
                  {codebaseInfo?.name ||
                    `Repository (${codebaseId.slice(0, 8)})`}
                </h1>
                {codebaseInfo?.branch && (
                  <p className="text-[11px] text-slate-400 flex items-center gap-1">
                    <GitBranch className="w-3 h-3 text-orange-400" />
                    <span>{codebaseInfo.branch}</span>
                  </p>
                )}
              </div>
            </div>
          </div>
        </header>

        {/* Chat Interface Container */}
        <main className="flex-1">
          <CodebaseChatContainer
            codebaseId={codebaseId}
            codebaseName={codebaseInfo?.name}
          />
        </main>
      </div>
    </div>
  )
}
