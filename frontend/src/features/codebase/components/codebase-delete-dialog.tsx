import { AlertTriangle, Loader2, X } from 'lucide-react'
import type { CodebaseResponse } from '../types/codebase.types'

interface CodebaseDeleteDialogProps {
  isOpen: boolean
  codebase: CodebaseResponse | null
  onClose: () => void
  onConfirm: () => void
  isDeleting?: boolean
}

export function CodebaseDeleteDialog({
  isOpen,
  codebase,
  onClose,
  onConfirm,
  isDeleting = false,
}: CodebaseDeleteDialogProps) {
  if (!isOpen || !codebase) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div
        className="w-full max-w-md bg-[#0F141E] border border-slate-800 rounded-3xl p-6 sm:p-8 shadow-2xl relative overflow-hidden space-y-6"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Top Glow Highlight Line */}
        <div className="absolute top-0 left-0 right-0 h-[2px] bg-gradient-to-r from-transparent via-red-500 to-transparent" />

        {/* Modal Header */}
        <div className="flex items-center justify-between pb-4 border-b border-slate-800/80">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-red-500/10 border border-red-500/20 text-red-400 flex items-center justify-center shrink-0">
              <AlertTriangle className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-extrabold text-white tracking-tight">
                Delete Codebase
              </h2>
              <p className="text-xs text-slate-400">
                Confirm deletion request
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            disabled={isDeleting}
            className="w-8 h-8 rounded-xl bg-slate-900 border border-slate-800 text-slate-400 hover:text-white flex items-center justify-center transition-all cursor-pointer disabled:opacity-50"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Body Content */}
        <div className="space-y-3">
          <p className="text-xs text-slate-300 leading-relaxed">
            Are you sure you want to permanently delete the codebase{' '}
            <span className="font-bold text-white bg-slate-900 px-2 py-0.5 rounded-lg border border-slate-800 font-mono">
              {codebase.name}
            </span>
            ?
          </p>
          <div className="p-3.5 rounded-2xl bg-red-500/10 border border-red-500/20 text-red-400 text-xs flex items-start gap-2.5">
            <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>
              This action cannot be undone. All indexed files and vectors will be permanently removed.
            </span>
          </div>
        </div>

        {/* Modal Footer Actions */}
        <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800/80">
          <button
            type="button"
            onClick={onClose}
            disabled={isDeleting}
            className="px-4 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-300 text-xs font-semibold transition-all cursor-pointer disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={isDeleting}
            className="px-5 py-2.5 rounded-xl bg-red-600 hover:bg-red-500 text-white text-xs font-semibold shadow-md shadow-red-950/40 flex items-center gap-2 transition-all cursor-pointer disabled:opacity-50"
          >
            {isDeleting ? (
              <>
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                <span>Deleting...</span>
              </>
            ) : (
              <span>Delete Codebase</span>
            )}
          </button>
        </div>
      </div>
    </div>
  )
}
