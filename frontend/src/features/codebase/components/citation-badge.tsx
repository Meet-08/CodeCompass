import { FileCode, Hash, ChevronRight } from 'lucide-react'
import { useState } from 'react'
import type { CodeCitation } from '../types/codebase.types'

interface CitationBadgeProps {
  citation: CodeCitation
}

export function CitationBadge({ citation }: CitationBadgeProps) {
  const [expanded, setExpanded] = useState(false)

  const lineRange =
    citation.startLine !== null && citation.endLine !== null
      ? `L${citation.startLine}-${citation.endLine}`
      : citation.startLine !== null
        ? `L${citation.startLine}`
        : ''

  // Distance formatting (score indicator)
  const scorePercentage = Math.max(
    0,
    Math.min(100, Math.round((1 - citation.distance) * 100)),
  )

  return (
    <div className="rounded-xl bg-slate-900/90 border border-slate-800 text-xs transition-all overflow-hidden">
      <button
        onClick={() => setExpanded(!expanded)}
        className="w-full p-2.5 flex items-center justify-between gap-2 hover:bg-slate-800/50 transition-colors text-left cursor-pointer"
      >
        <div className="flex items-center gap-2 min-w-0">
          <FileCode className="w-3.5 h-3.5 text-orange-400 shrink-0" />
          <span className="font-mono text-[11px] text-slate-200 truncate">
            {citation.path}
          </span>
          {lineRange && (
            <span className="px-1.5 py-0.5 rounded bg-slate-800 text-slate-400 font-mono text-[10px] shrink-0">
              {lineRange}
            </span>
          )}
        </div>

        <div className="flex items-center gap-2 shrink-0">
          {citation.language && (
            <span className="px-2 py-0.5 rounded-md bg-orange-500/10 text-orange-400 text-[10px] font-semibold uppercase">
              {citation.language}
            </span>
          )}
          <ChevronRight
            className={`w-3.5 h-3.5 text-slate-400 transition-transform ${expanded ? 'rotate-90' : ''}`}
          />
        </div>
      </button>

      {expanded && (
        <div className="p-3 border-t border-slate-800/80 bg-slate-950/60 space-y-2 text-[11px]">
          <div className="grid grid-cols-2 gap-2 text-slate-400">
            <div>
              <span className="text-slate-500">Language:</span>{' '}
              <span className="text-slate-300 font-mono">
                {citation.language || 'N/A'}
              </span>
            </div>
            <div>
              <span className="text-slate-500">Relevance Score:</span>{' '}
              <span className="text-emerald-400 font-semibold">
                {scorePercentage}%
              </span>
            </div>
            <div className="col-span-2 flex items-center gap-1.5 text-slate-500">
              <Hash className="w-3 h-3" />
              <span className="font-mono text-[10px] truncate">
                Chunk ID: {citation.chunkId}
              </span>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
