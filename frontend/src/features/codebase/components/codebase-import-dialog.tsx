import { useState } from 'react'
import {
  X,
  GitBranch,
  FolderPlus,
  Link as LinkIcon,
  AlertCircle,
  Loader2,
} from 'lucide-react'
import { toast } from 'sonner'
import { useImportCodebase } from '../hooks/use-codebase'
import { getApiErrorMessage } from '#/lib/utils'

interface CodebaseImportDialogProps {
  isOpen: boolean
  onClose: () => void
  onSuccess: () => void
}

export function CodebaseImportDialog({
  isOpen,
  onClose,
  onSuccess,
}: CodebaseImportDialogProps) {
  const [name, setName] = useState('')
  const [cloneUrl, setCloneUrl] = useState('')
  const [branch, setBranch] = useState('main')
  const [formError, setFormError] = useState<string | null>(null)

  const importMutation = useImportCodebase()

  if (!isOpen) return null

  const handleSubmit = (e: React.SyntheticEvent) => {
    e.preventDefault()
    setFormError(null)

    if (!name.trim()) {
      setFormError('Repository display name is required')
      return
    }

    if (!cloneUrl.trim()) {
      setFormError('HTTPS Clone URL is required')
      return
    }

    if (!/^https:\/\/.+$/i.test(cloneUrl.trim())) {
      setFormError('Clone URL must start with https://')
      return
    }

    importMutation.mutate(
      {
        name: name.trim(),
        cloneUrl: cloneUrl.trim(),
        branch: branch.trim() || 'main',
      },
      {
        onSuccess: (response) => {
          if (response.success && response.data) {
            toast.success('Codebase import queued successfully!')
            setName('')
            setCloneUrl('')
            setBranch('main')
            onSuccess()
          } else {
            setFormError(response.message || 'Failed to queue codebase import')
          }
        },
        onError: (err) => {
          const msg = getApiErrorMessage(err, 'Failed to import codebase')
          setFormError(msg)
          toast.error(msg)
        },
      },
    )
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div
        className="w-full max-w-lg bg-[#0F141E] border border-slate-800 rounded-3xl p-6 sm:p-8 shadow-2xl relative overflow-hidden space-y-6"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Glow Header */}
        <div className="absolute top-0 left-0 right-0 h-[2px] bg-gradient-to-r from-transparent via-orange-500 to-transparent" />

        {/* Modal Header */}
        <div className="flex items-center justify-between pb-4 border-b border-slate-800/80">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-orange-500/10 border border-orange-500/20 text-orange-400 flex items-center justify-center">
              <FolderPlus className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-extrabold text-white tracking-tight">
                Import Codebase
              </h2>
              <p className="text-xs text-slate-400">
                Queue repository indexing for code-aware AI chat
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-xl bg-slate-900 border border-slate-800 text-slate-400 hover:text-white flex items-center justify-center transition-all cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Error Alert */}
        {formError && (
          <div className="p-3.5 rounded-2xl bg-red-500/10 border border-red-500/20 text-red-400 text-xs flex items-start gap-2.5">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{formError}</span>
          </div>
        )}

        {/* Import Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300">
              Repository Name <span className="text-orange-500">*</span>
            </label>
            <input
              type="text"
              placeholder="e.g. Server Repository"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-900/80 border border-slate-800 text-slate-100 text-xs focus:outline-none focus:border-orange-500/50 transition-all placeholder:text-slate-500"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
              <LinkIcon className="w-3.5 h-3.5 text-slate-400" />
              <span>Clone URL (HTTPS)</span>
              <span className="text-orange-500">*</span>
            </label>
            <input
              type="text"
              placeholder="https://github.com/example/server.git"
              value={cloneUrl}
              onChange={(e) => setCloneUrl(e.target.value)}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-900/80 border border-slate-800 text-slate-100 text-xs focus:outline-none focus:border-orange-500/50 transition-all placeholder:text-slate-500"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
              <GitBranch className="w-3.5 h-3.5 text-slate-400" />
              <span>Branch</span>
              <span className="text-slate-500 font-normal">
                (Defaults to main)
              </span>
            </label>
            <input
              type="text"
              placeholder="main"
              value={branch}
              onChange={(e) => setBranch(e.target.value)}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-900/80 border border-slate-800 text-slate-100 text-xs focus:outline-none focus:border-orange-500/50 transition-all placeholder:text-slate-500"
            />
          </div>

          {/* Form Actions */}
          <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800/80">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-300 text-xs font-semibold transition-all cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={importMutation.isPending}
              className="px-5 py-2.5 rounded-xl bg-orange-600 hover:bg-orange-500 text-white text-xs font-semibold shadow-md shadow-orange-950/40 flex items-center gap-2 transition-all cursor-pointer disabled:opacity-50"
            >
              {importMutation.isPending ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  <span>Queueing...</span>
                </>
              ) : (
                <span>Queue Import</span>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
