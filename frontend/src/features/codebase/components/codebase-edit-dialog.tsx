import { useState, useEffect } from 'react'
import {
  X,
  GitBranch,
  Pencil,
  AlertCircle,
  Loader2,
} from 'lucide-react'
import { toast } from 'sonner'
import { useUpdateCodebase } from '../hooks/use-codebase'
import { getApiErrorMessage } from '#/lib/utils'
import type { CodebaseResponse } from '../types/codebase.types'

interface CodebaseEditDialogProps {
  isOpen: boolean
  codebase: CodebaseResponse
  onClose: () => void
}

export function CodebaseEditDialog({
  isOpen,
  codebase,
  onClose,
}: CodebaseEditDialogProps) {
  const [name, setName] = useState(codebase.name)
  const [branch, setBranch] = useState(codebase.branch || 'main')
  const [formError, setFormError] = useState<string | null>(null)

  const updateMutation = useUpdateCodebase()

  // Reset form values when codebase changes or dialog opens
  useEffect(() => {
    if (isOpen) {
      setName(codebase.name)
      setBranch(codebase.branch || 'main')
      setFormError(null)
    }
  }, [isOpen, codebase])

  if (!isOpen) return null

  const handleSubmit = (e: React.SyntheticEvent) => {
    e.preventDefault()
    setFormError(null)

    if (!name.trim()) {
      setFormError('Display name is required')
      return
    }

    if (!branch.trim()) {
      setFormError('Branch name is required')
      return
    }

    updateMutation.mutate(
      {
        codebaseId: codebase.codebaseId,
        data: {
          name: name.trim(),
          branch: branch.trim(),
        },
      },
      {
        onSuccess: (response) => {
          if (response.success) {
            toast.success('Codebase updated successfully!')
            onClose()
          } else {
            setFormError(response.message || 'Failed to update codebase')
          }
        },
        onError: (err) => {
          const msg = getApiErrorMessage(err, 'Failed to update codebase')
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
        <div className="absolute top-0 left-0 right-0 h-[2px] bg-gradient-to-r from-transparent via-cyan-500 to-transparent" />

        {/* Modal Header */}
        <div className="flex items-center justify-between pb-4 border-b border-slate-800/80">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-cyan-500/10 border border-cyan-500/20 text-cyan-400 flex items-center justify-center">
              <Pencil className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-extrabold text-white tracking-tight">
                Edit Codebase
              </h2>
              <p className="text-xs text-slate-400">
                Update display name and branch (clone URL cannot be changed)
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

        {/* Edit Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300">
              Display Name <span className="text-orange-500">*</span>
            </label>
            <input
              type="text"
              placeholder="e.g. Server Repository"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-900/80 border border-slate-800 text-slate-100 text-xs focus:outline-none focus:border-cyan-500/50 transition-all placeholder:text-slate-500"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
              <GitBranch className="w-3.5 h-3.5 text-slate-400" />
              <span>Branch</span>
              <span className="text-orange-500">*</span>
            </label>
            <input
              type="text"
              placeholder="main"
              value={branch}
              onChange={(e) => setBranch(e.target.value)}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-900/80 border border-slate-800 text-slate-100 text-xs focus:outline-none focus:border-cyan-500/50 transition-all placeholder:text-slate-500"
            />
          </div>

          {/* Clone URL read-only display */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-500">
              Clone URL (read-only)
            </label>
            <div className="w-full px-4 py-2.5 rounded-xl bg-slate-900/50 border border-slate-800/50 text-slate-500 text-xs font-mono truncate">
              {codebase.cloneUrl || 'N/A'}
            </div>
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
              disabled={updateMutation.isPending}
              className="px-5 py-2.5 rounded-xl bg-cyan-600 hover:bg-cyan-500 text-white text-xs font-semibold shadow-md shadow-cyan-950/40 flex items-center gap-2 transition-all cursor-pointer disabled:opacity-50"
            >
              {updateMutation.isPending ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  <span>Saving...</span>
                </>
              ) : (
                <span>Save Changes</span>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
