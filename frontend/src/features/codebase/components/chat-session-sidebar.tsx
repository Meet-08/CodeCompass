import { useState } from 'react'
import {
  Plus,
  MessageSquare,
  Pencil,
  Trash2,
  Check,
  X,
  Loader2,
  ChevronLeft,
  ChevronRight,
  Clock,
} from 'lucide-react'
import { toast } from 'sonner'
import {
  useChatSessions,
  useUpdateChatSession,
  useDeleteChatSession,
} from '#/features/chat'
import { getApiErrorMessage } from '#/lib/utils'

interface ChatSessionSidebarProps {
  codebaseId: string
  activeSessionId: string | null
  onSelectSession: (sessionId: string) => void
  onNewChat: () => void
  isCollapsed: boolean
  onToggleCollapse: () => void
}

export function ChatSessionSidebar({
  codebaseId,
  activeSessionId,
  onSelectSession,
  onNewChat,
  isCollapsed,
  onToggleCollapse,
}: ChatSessionSidebarProps) {
  const { data: sessionsResponse, isLoading } = useChatSessions(codebaseId)
  const updateMutation = useUpdateChatSession(codebaseId)
  const deleteMutation = useDeleteChatSession(codebaseId)

  const [editingId, setEditingId] = useState<string | null>(null)
  const [editTitle, setEditTitle] = useState('')
  const [deletingId, setDeletingId] = useState<string | null>(null)

  const sessions = sessionsResponse?.data ?? []

  const handleStartEdit = (sessionId: string, currentTitle: string) => {
    setEditingId(sessionId)
    setEditTitle(currentTitle)
  }

  const handleSaveEdit = (sessionId: string) => {
    const trimmed = editTitle.trim()
    if (!trimmed) {
      toast.error('Title cannot be empty')
      return
    }

    updateMutation.mutate(
      { sessionId, data: { title: trimmed } },
      {
        onSuccess: (res) => {
          if (res.success) {
            toast.success('Session renamed')
            setEditingId(null)
          } else {
            toast.error(res.message || 'Failed to rename session')
          }
        },
        onError: (err) => {
          toast.error(getApiErrorMessage(err, 'Failed to rename session'))
        },
      },
    )
  }

  const handleDelete = (sessionId: string) => {
    deleteMutation.mutate(sessionId, {
      onSuccess: () => {
        toast.success('Session deleted')
        setDeletingId(null)
        if (activeSessionId === sessionId) {
          onNewChat()
        }
      },
      onError: (err) => {
        toast.error(getApiErrorMessage(err, 'Failed to delete session'))
        setDeletingId(null)
      },
    })
  }

  const formatRelativeTime = (dateStr: string) => {
    try {
      const date = new Date(dateStr)
      const now = new Date()
      const diffMs = now.getTime() - date.getTime()
      const diffMins = Math.floor(diffMs / 60000)
      const diffHours = Math.floor(diffMins / 60)
      const diffDays = Math.floor(diffHours / 24)

      if (diffMins < 1) return 'Just now'
      if (diffMins < 60) return `${diffMins}m ago`
      if (diffHours < 24) return `${diffHours}h ago`
      if (diffDays < 7) return `${diffDays}d ago`
      return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
    } catch {
      return ''
    }
  }

  // Collapsed state — thin rail with just icons
  if (isCollapsed) {
    return (
      <div className="w-12 flex flex-col bg-[#0A0E16] border-r border-slate-800/80 shrink-0">
        <button
          onClick={onToggleCollapse}
          className="p-3 text-slate-400 hover:text-white transition-colors cursor-pointer"
          title="Expand sidebar"
        >
          <ChevronRight className="w-4 h-4" />
        </button>
        <button
          onClick={onNewChat}
          className="p-3 text-cyan-400 hover:text-cyan-300 transition-colors cursor-pointer"
          title="New chat"
        >
          <Plus className="w-4 h-4" />
        </button>
        <div className="flex-1 overflow-hidden py-2 space-y-1">
          {sessions.slice(0, 10).map((s) => (
            <button
              key={s.sessionId}
              onClick={() => onSelectSession(s.sessionId)}
              className={`w-full p-3 transition-colors cursor-pointer ${
                activeSessionId === s.sessionId
                  ? 'text-cyan-400 bg-cyan-500/10'
                  : 'text-slate-500 hover:text-slate-300'
              }`}
              title={s.title}
            >
              <MessageSquare className="w-3.5 h-3.5" />
            </button>
          ))}
        </div>
      </div>
    )
  }

  return (
    <div className="w-72 flex flex-col bg-[#0A0E16] border-r border-slate-800/80 shrink-0">
      {/* Sidebar Header */}
      <div className="p-3 border-b border-slate-800/80 flex items-center justify-between">
        <h3 className="text-xs font-bold text-slate-300 uppercase tracking-wider">
          Chat Sessions
        </h3>
        <div className="flex items-center gap-1">
          <button
            onClick={onNewChat}
            className="p-1.5 rounded-lg bg-cyan-500/10 hover:bg-cyan-500/20 border border-cyan-500/20 text-cyan-400 hover:text-cyan-300 transition-all cursor-pointer"
            title="New chat"
          >
            <Plus className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={onToggleCollapse}
            className="p-1.5 rounded-lg bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-400 hover:text-white transition-all cursor-pointer"
            title="Collapse sidebar"
          >
            <ChevronLeft className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* Sessions List */}
      <div className="flex-1 overflow-y-auto py-2 space-y-0.5 scrollbar-thin scrollbar-thumb-slate-800">
        {isLoading ? (
          <div className="flex items-center justify-center py-8">
            <Loader2 className="w-4 h-4 animate-spin text-slate-500" />
          </div>
        ) : sessions.length === 0 ? (
          <div className="px-3 py-8 text-center">
            <MessageSquare className="w-6 h-6 text-slate-700 mx-auto mb-2" />
            <p className="text-[11px] text-slate-500">
              No chat sessions yet.
            </p>
            <p className="text-[10px] text-slate-600 mt-0.5">
              Send a message to start one.
            </p>
          </div>
        ) : (
          sessions.map((session) => {
            const isActive = activeSessionId === session.sessionId
            const isEditing = editingId === session.sessionId
            const isDeleting = deletingId === session.sessionId

            return (
              <div
                key={session.sessionId}
                className={`group mx-1.5 rounded-xl transition-all ${
                  isActive
                    ? 'bg-cyan-500/10 border border-cyan-500/20'
                    : 'hover:bg-slate-900/80 border border-transparent'
                }`}
              >
                {/* Delete confirmation overlay */}
                {isDeleting ? (
                  <div className="p-2.5 space-y-2">
                    <p className="text-[11px] text-red-400 font-medium">
                      Delete this session?
                    </p>
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => handleDelete(session.sessionId)}
                        disabled={deleteMutation.isPending}
                        className="flex-1 py-1 px-2 rounded-lg bg-red-600/80 hover:bg-red-500 text-white text-[10px] font-semibold transition-all cursor-pointer disabled:opacity-50 flex items-center justify-center gap-1"
                      >
                        {deleteMutation.isPending ? (
                          <Loader2 className="w-3 h-3 animate-spin" />
                        ) : (
                          'Delete'
                        )}
                      </button>
                      <button
                        onClick={() => setDeletingId(null)}
                        className="flex-1 py-1 px-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-[10px] font-semibold transition-all cursor-pointer"
                      >
                        Cancel
                      </button>
                    </div>
                  </div>
                ) : isEditing ? (
                  /* Edit mode */
                  <div className="p-2 flex items-center gap-1.5">
                    <input
                      type="text"
                      value={editTitle}
                      onChange={(e) => setEditTitle(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') handleSaveEdit(session.sessionId)
                        if (e.key === 'Escape') setEditingId(null)
                      }}
                      autoFocus
                      className="flex-1 px-2 py-1 rounded-lg bg-slate-900 border border-slate-700 text-slate-100 text-[11px] focus:outline-none focus:border-cyan-500/50"
                    />
                    <button
                      onClick={() => handleSaveEdit(session.sessionId)}
                      disabled={updateMutation.isPending}
                      className="p-1 rounded-md text-emerald-400 hover:bg-emerald-500/10 transition-colors cursor-pointer"
                    >
                      {updateMutation.isPending ? (
                        <Loader2 className="w-3 h-3 animate-spin" />
                      ) : (
                        <Check className="w-3 h-3" />
                      )}
                    </button>
                    <button
                      onClick={() => setEditingId(null)}
                      className="p-1 rounded-md text-slate-400 hover:bg-slate-800 transition-colors cursor-pointer"
                    >
                      <X className="w-3 h-3" />
                    </button>
                  </div>
                ) : (
                  /* Normal session item */
                  <div
                    onClick={() => onSelectSession(session.sessionId)}
                    role="button"
                    tabIndex={0}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' || e.key === ' ') {
                        onSelectSession(session.sessionId)
                      }
                    }}
                    className="w-full p-2.5 flex items-start gap-2.5 text-left cursor-pointer outline-none focus-visible:ring-1 focus-visible:ring-cyan-500/50 rounded-xl"
                  >
                    <MessageSquare
                      className={`w-3.5 h-3.5 mt-0.5 shrink-0 ${
                        isActive ? 'text-cyan-400' : 'text-slate-500'
                      }`}
                    />
                    <div className="flex-1 min-w-0">
                      <p
                        className={`text-[12px] font-semibold truncate ${
                          isActive ? 'text-cyan-300' : 'text-slate-200'
                        }`}
                      >
                        {session.title}
                      </p>
                      <p className="text-[10px] text-slate-500 flex items-center gap-1 mt-0.5">
                        <Clock className="w-2.5 h-2.5" />
                        {formatRelativeTime(session.updatedAt)}
                      </p>
                    </div>

                    {/* Action buttons (visible on hover or active) */}
                    <div
                      className={`flex items-center gap-0.5 shrink-0 transition-opacity ${
                        isActive
                          ? 'opacity-100'
                          : 'opacity-0 group-hover:opacity-100'
                      }`}
                    >
                      <button
                        onClick={(e) => {
                          e.stopPropagation()
                          handleStartEdit(session.sessionId, session.title)
                        }}
                        className="p-1 rounded-md text-slate-400 hover:text-cyan-400 hover:bg-cyan-500/10 transition-all cursor-pointer"
                        title="Rename"
                      >
                        <Pencil className="w-3 h-3" />
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation()
                          setDeletingId(session.sessionId)
                        }}
                        className="p-1 rounded-md text-slate-400 hover:text-red-400 hover:bg-red-500/10 transition-all cursor-pointer"
                        title="Delete"
                      >
                        <Trash2 className="w-3 h-3" />
                      </button>
                    </div>
                  </div>
                )}
              </div>
            )
          })
        )}
      </div>

      {/* Session Count Footer */}
      {sessions.length > 0 && (
        <div className="p-3 border-t border-slate-800/80 text-center">
          <p className="text-[10px] text-slate-500">
            {sessions.length} session{sessions.length !== 1 ? 's' : ''}
          </p>
        </div>
      )}
    </div>
  )
}
