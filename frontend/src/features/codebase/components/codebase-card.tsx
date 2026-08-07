import { Link } from '@tanstack/react-router'
import {
  GitBranch,
  MessageSquare,
  ExternalLink,
  FileCode,
  CheckCircle2,
  Clock,
  AlertTriangle,
  Loader2,
  Trash2,
  Pencil,
  RotateCw,
  GitCommitHorizontal,
  Calendar,
} from 'lucide-react'
import type { CodebaseResponse } from '../types/codebase.types'

interface CodebaseCardProps {
  codebase: CodebaseResponse
  onDelete?: (codebaseId: string) => void
  onReindex?: (codebaseId: string) => void
  onEdit?: (codebase: CodebaseResponse) => void
  isDeleting?: boolean
  isReindexing?: boolean
}

export function CodebaseCard({
  codebase,
  onDelete,
  onReindex,
  onEdit,
  isDeleting,
  isReindexing,
}: CodebaseCardProps) {
  const isIndexed = codebase.status === 'INDEXED'
  const isActive =
    codebase.status === 'QUEUED' || codebase.status === 'PROCESSING'

  const renderStatusBadge = () => {
    switch (codebase.status) {
      case 'INDEXED':
        return (
          <span className="px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 flex items-center gap-1.5">
            <CheckCircle2 className="w-3 h-3" />
            Indexed
          </span>
        )
      case 'QUEUED':
        return (
          <span className="px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider bg-amber-500/10 text-amber-400 border border-amber-500/20 flex items-center gap-1.5">
            <Clock className="w-3 h-3 animate-pulse" />
            Queued
          </span>
        )
      case 'PROCESSING':
        return (
          <span className="px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider bg-cyan-500/10 text-cyan-400 border border-cyan-500/20 flex items-center gap-1.5">
            <Loader2 className="w-3 h-3 animate-spin" />
            Processing
          </span>
        )
      case 'FAILED':
        return (
          <span className="px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider bg-red-500/10 text-red-400 border border-red-500/20 flex items-center gap-1.5">
            <AlertTriangle className="w-3 h-3" />
            Failed
          </span>
        )
      default:
        return null
    }
  }

  const formatDate = (dateStr: string | null) => {
    if (!dateStr) return null
    try {
      return new Date(dateStr).toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
    } catch {
      return null
    }
  }

  return (
    <div className="p-6 rounded-3xl bg-[#0F141E]/90 border border-slate-800/80 hover:border-slate-700/80 shadow-xl transition-all relative overflow-hidden group flex flex-col justify-between space-y-5">
      {/* Dynamic top highlight line */}
      <div className="absolute top-0 left-0 right-0 h-[2px] bg-gradient-to-r from-transparent via-slate-700 group-hover:via-orange-500/80 to-transparent transition-all duration-300" />

      {/* Card Header */}
      <div className="space-y-3">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-slate-900 border border-slate-800 text-orange-400 flex items-center justify-center shrink-0">
              <FileCode className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-extrabold text-white tracking-tight line-clamp-1">
                {codebase.name}
              </h3>
            </div>
          </div>
          {renderStatusBadge()}
        </div>

        {/* Info Pills */}
        <div className="flex flex-wrap items-center gap-2 text-xs pt-1">
          <div className="px-2.5 py-1 rounded-xl bg-slate-900/80 border border-slate-800 text-slate-300 flex items-center gap-1.5 text-[11px]">
            <GitBranch className="w-3 h-3 text-orange-400" />
            <span>{codebase.branch || 'main'}</span>
          </div>

          <div className="px-2.5 py-1 rounded-xl bg-slate-900/80 border border-slate-800 text-slate-300 flex items-center gap-1.5 text-[11px]">
            <FileCode className="w-3 h-3 text-cyan-400" />
            <span>
              {codebase.fileCount} {codebase.fileCount === 1 ? 'file' : 'files'}
            </span>
          </div>

          {codebase.lastCommitSha && (
            <div className="px-2.5 py-1 rounded-xl bg-slate-900/80 border border-slate-800 text-slate-300 flex items-center gap-1.5 text-[11px]">
              <GitCommitHorizontal className="w-3 h-3 text-violet-400" />
              <span className="font-mono">
                {codebase.lastCommitSha.slice(0, 7)}
              </span>
            </div>
          )}
        </div>

        {/* Clone URL link */}
        {codebase.cloneUrl && (
          <div className="text-xs text-slate-400 truncate flex items-center gap-1.5 pt-1">
            <ExternalLink className="w-3 h-3 text-slate-500 shrink-0" />
            <a
              href={codebase.cloneUrl}
              target="_blank"
              rel="noreferrer"
              className="hover:text-orange-400 transition-colors truncate font-mono text-[11px]"
            >
              {codebase.cloneUrl}
            </a>
          </div>
        )}

        {/* Timestamps */}
        {(codebase.indexedAt || codebase.createdAt) && (
          <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-[10px] text-slate-500 pt-1">
            {codebase.indexedAt && (
              <span className="flex items-center gap-1">
                <Calendar className="w-3 h-3" />
                Indexed: {formatDate(codebase.indexedAt)}
              </span>
            )}
            {codebase.createdAt && !codebase.indexedAt && (
              <span className="flex items-center gap-1">
                <Calendar className="w-3 h-3" />
                Created: {formatDate(codebase.createdAt)}
              </span>
            )}
          </div>
        )}
      </div>

      {/* Action Footer */}
      <div className="flex items-center justify-between pt-4 border-t border-slate-800/80 gap-2">
        <div className="flex items-center gap-2">
          {onDelete && (
            <button
              onClick={() => onDelete(codebase.codebaseId)}
              disabled={isDeleting || isActive}
              className="p-2.5 rounded-xl bg-red-500/10 hover:bg-red-500/20 border border-red-500/20 hover:border-red-500/40 text-red-400 hover:text-red-300 shadow-sm shadow-red-950/20 active:scale-95 transition-all cursor-pointer disabled:opacity-30 disabled:cursor-not-allowed disabled:bg-slate-900/50 disabled:border-slate-800/50 disabled:text-slate-600 disabled:shadow-none disabled:active:scale-100"
              title={
                isActive ? 'Cannot delete while indexing' : 'Delete codebase'
              }
            >
              {isDeleting ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <Trash2 className="w-4 h-4" />
              )}
            </button>
          )}

          {onEdit && (
            <button
              onClick={() => onEdit(codebase)}
              className="p-2.5 rounded-xl bg-cyan-500/10 hover:bg-cyan-500/20 border border-cyan-500/20 hover:border-cyan-500/40 text-cyan-400 hover:text-cyan-300 shadow-sm shadow-cyan-950/20 active:scale-95 transition-all cursor-pointer"
              title="Edit codebase"
            >
              <Pencil className="w-4 h-4" />
            </button>
          )}

          {onReindex && (isIndexed || codebase.status === 'FAILED') && (
            <button
              onClick={() => onReindex(codebase.codebaseId)}
              disabled={isReindexing}
              className="p-2.5 rounded-xl bg-violet-500/10 hover:bg-violet-500/20 border border-violet-500/20 hover:border-violet-500/40 text-violet-400 hover:text-violet-300 shadow-sm shadow-violet-950/20 active:scale-95 transition-all cursor-pointer disabled:opacity-30 disabled:cursor-not-allowed disabled:bg-slate-900/50 disabled:border-slate-800/50 disabled:text-slate-600 disabled:shadow-none disabled:active:scale-100"
              title="Reindex codebase"
            >
              {isReindexing ? (
                <Loader2 className="w-4 h-4 animate-spin text-violet-400" />
              ) : (
                <RotateCw className="w-4 h-4" />
              )}
            </button>
          )}
        </div>

        <Link
          to="/codebases/$codebaseId/chat"
          params={{ codebaseId: codebase.codebaseId }}
          className={`flex-1 py-2.5 px-4 rounded-xl !text-white font-semibold text-xs flex items-center justify-center gap-2 shadow-md transition-all ${
            isIndexed
              ? 'bg-orange-600 hover:bg-orange-500 shadow-orange-950/30 group-hover:shadow-orange-950/60'
              : 'bg-slate-800 !text-slate-400 border border-slate-800 cursor-not-allowed pointer-events-none opacity-50'
          }`}
          disabled={!isIndexed}
        >
          <MessageSquare className="w-3.5 h-3.5 text-white" />
          <span className="text-white">Chat with Codebase</span>
        </Link>
      </div>
    </div>
  )
}
