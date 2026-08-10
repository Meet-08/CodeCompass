import { useState, useCallback } from 'react'
import { createFileRoute, Link } from '@tanstack/react-router'
import { ArrowLeft, GitBranch, FolderGit2, FileCode } from 'lucide-react'
import {
  CodebaseChatContainer,
  ChatSessionSidebar,
  useCodebases,
} from '#/features/codebase'

export const Route = createFileRoute('/_app/codebases/$codebaseId/chat')({
  component: CodebaseChatPage,
})

function CodebaseChatPage() {
  const { codebaseId } = Route.useParams()
  const { data: codebasesResponse } = useCodebases()

  const [activeSessionId, setActiveSessionId] = useState<string | null>(null)
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)

  const codebaseInfo = codebasesResponse?.data?.find(
    (c) => c.codebaseId === codebaseId,
  )

  const handleSelectSession = useCallback((sessionId: string) => {
    setActiveSessionId(sessionId)
  }, [])

  const handleNewChat = useCallback(() => {
    setActiveSessionId(null)
  }, [])

  const handleSessionResolved = useCallback((sessionId: string) => {
    setActiveSessionId(sessionId)
  }, [])

  return (
    <div className="h-screen bg-[#080B11] text-slate-100 font-sans relative overflow-hidden flex flex-col">

      {/* Top Navigation Bar */}
      <header className="flex items-center justify-between py-2.5 px-4 sm:px-6 bg-[#0A0E16]/90 border-b border-slate-800/80 z-10 shrink-0">
        <div className="flex items-center gap-3">
          <Link
            to="/codebases"
            className="p-2 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-400 hover:text-slate-200 transition-all cursor-pointer flex items-center gap-1.5 text-xs font-semibold"
          >
            <ArrowLeft className="w-4 h-4" />
            <span className="hidden sm:inline">Back to Codebases</span>
          </Link>

          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-xl bg-slate-900 border border-slate-800 text-cyan-400 flex items-center justify-center">
              <FolderGit2 className="w-4 h-4" />
            </div>
            <div>
              <h1 className="text-sm font-extrabold text-white tracking-tight flex items-center gap-2">
                {codebaseInfo?.name ||
                  `Repository (${codebaseId.slice(0, 8)})`}
              </h1>
              <div className="flex items-center gap-3">
                {codebaseInfo?.branch && (
                  <p className="text-[11px] text-slate-400 flex items-center gap-1">
                    <GitBranch className="w-3 h-3 text-cyan-400" />
                    <span>{codebaseInfo.branch}</span>
                  </p>
                )}
                {codebaseInfo?.fileCount != null && (
                  <p className="text-[11px] text-slate-500 flex items-center gap-1">
                    <FileCode className="w-3 h-3 text-cyan-400" />
                    <span>
                      {codebaseInfo.fileCount}{' '}
                      {codebaseInfo.fileCount === 1 ? 'file' : 'files'}
                    </span>
                  </p>
                )}
              </div>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content — Sidebar + Chat Container */}
      <div className="flex-1 flex min-h-0 relative z-10">
        {/* Chat Session Sidebar */}
        <ChatSessionSidebar
          codebaseId={codebaseId}
          activeSessionId={activeSessionId}
          onSelectSession={handleSelectSession}
          onNewChat={handleNewChat}
          isCollapsed={sidebarCollapsed}
          onToggleCollapse={() => setSidebarCollapsed((prev) => !prev)}
        />

        {/* Chat Interface Container */}
        <main className="flex-1 min-w-0 p-3 sm:p-4">
          <CodebaseChatContainer
            codebaseId={codebaseId}
            codebaseName={codebaseInfo?.name}
            sessionId={activeSessionId}
            onSessionResolved={handleSessionResolved}
          />
        </main>
      </div>
    </div>
  )
}
