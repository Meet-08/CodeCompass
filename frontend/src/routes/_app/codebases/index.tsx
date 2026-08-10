import { useState } from 'react'
import { createFileRoute, Link } from '@tanstack/react-router'
import {
  FolderPlus,
  Search,
  FileCode,
  Sparkles,
  Layers,
  ArrowLeft,
  RefreshCw,
  Loader2,
  AlertCircle,
} from 'lucide-react'
import { toast } from 'sonner'
import {
  CodebaseCard,
  CodebaseImportDialog,
  CodebaseEditDialog,
  CodebaseDeleteDialog,
  useCodebases,
  useDeleteCodebase,
  useReindexCodebase
  
} from '#/features/codebase'
import type {CodebaseResponse} from '#/features/codebase';
import { getApiErrorMessage } from '#/lib/utils'

export const Route = createFileRoute('/_app/codebases/')({
  component: CodebasesIndexPage,
})

const MAX_CODEBASES = 5

function CodebasesIndexPage() {
  const [searchQuery, setSearchQuery] = useState('')
  const [isImportModalOpen, setIsImportModalOpen] = useState(false)
  const [editingCodebase, setEditingCodebase] =
    useState<CodebaseResponse | null>(null)
  const [deletingCodebase, setDeletingCodebase] =
    useState<CodebaseResponse | null>(null)

  const {
    data: codebasesResponse,
    isLoading,
    isError,
    error: queryError,
    refetch,
    isFetching,
  } = useCodebases()

  const deleteMutation = useDeleteCodebase()
  const reindexMutation = useReindexCodebase()

  const codebases = codebasesResponse?.data ?? []

  const handleImportClick = () => {
    if (codebases.length >= MAX_CODEBASES) {
      toast.error(
        `You can have at most ${MAX_CODEBASES} codebases. Delete an existing one first.`,
      )
      return
    }
    setIsImportModalOpen(true)
  }

  const handleDeleteCodebase = (codebaseId: string) => {
    const codebase = codebases.find((c) => c.codebaseId === codebaseId)
    if (!codebase) return

    if (codebase.status === 'QUEUED' || codebase.status === 'PROCESSING') {
      toast.error('Cannot delete a codebase that is currently being indexed.')
      return
    }

    setDeletingCodebase(codebase)
  }

  const handleConfirmDelete = () => {
    if (!deletingCodebase) return

    deleteMutation.mutate(deletingCodebase.codebaseId, {
      onSuccess: () => {
        toast.success('Codebase deleted successfully.')
        setDeletingCodebase(null)
      },
      onError: (err) => {
        const msg = getApiErrorMessage(err, 'Failed to delete codebase')
        toast.error(msg)
      },
    })
  }

  const handleReindexCodebase = (codebaseId: string) => {
    reindexMutation.mutate(codebaseId, {
      onSuccess: (response) => {
        if (response.success) {
          toast.success(
            'Codebase reindex queued. Use the refresh button to check progress.',
          )
        } else {
          toast.error(response.message || 'Failed to queue reindex')
        }
      },
      onError: (err) => {
        const msg = getApiErrorMessage(err, 'Failed to reindex codebase')
        toast.error(msg)
      },
    })
  }

  const handleRefresh = () => {
    refetch()
    toast.info('Refreshing codebases...', { id: 'refresh-codebases' })
  }

  const filteredCodebases = codebases.filter(
    (c) =>
      c.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (c.cloneUrl?.toLowerCase().includes(searchQuery.toLowerCase()) ??
        false) ||
      (c.branch?.toLowerCase().includes(searchQuery.toLowerCase()) ?? false),
  )

  const totalIndexedFiles = codebases.reduce(
    (acc, c) => acc + (c.fileCount || 0),
    0,
  )
  const indexedReposCount = codebases.filter(
    (c) => c.status === 'INDEXED',
  ).length

  return (
    <div className="min-h-screen bg-[#080B11] text-slate-100 p-6 sm:p-12 font-sans relative overflow-hidden">
      {/* Background Ambient Glows */}
      <div className="absolute top-0 left-0 w-full h-full pointer-events-none z-0">
        <div className="absolute top-[-10%] right-[-10%] w-[50%] h-[50%] bg-orange-500/10 blur-[140px] rounded-full" />
        <div className="absolute bottom-[-10%] left-[-10%] w-[50%] h-[50%] bg-cyan-500/10 blur-[140px] rounded-full" />
      </div>

      <div className="max-w-6xl mx-auto relative z-10 space-y-8">
        {/* Navigation & Header */}
        <header className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-slate-800/80">
          <div className="flex items-center gap-4">
            <Link
              to="/"
              className="p-2.5 rounded-2xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-400 hover:text-slate-200 transition-all cursor-pointer"
              title="Return to Home"
            >
              <ArrowLeft className="w-5 h-5" />
            </Link>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-2xl font-extrabold text-white tracking-tight">
                  Codebase Intelligence
                </h1>
                <span className="inline-block w-2.5 h-2.5 rounded-full bg-orange-500 animate-pulse" />
              </div>
              <p className="text-xs text-slate-400 mt-1">
                Manage indexed code repositories and perform AI-assisted
                architectural queries
              </p>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            {/* Refresh Button */}
            <button
              onClick={handleRefresh}
              disabled={isFetching}
              className="py-3 px-4 rounded-2xl bg-slate-900 hover:bg-slate-800 border border-slate-700/80 text-slate-300 hover:text-white font-semibold text-xs flex items-center justify-center gap-2 transition-all cursor-pointer disabled:opacity-50"
              title="Refresh codebases status"
            >
              <RefreshCw
                className={`w-4 h-4 ${isFetching ? 'animate-spin' : ''}`}
              />
              <span>Refresh</span>
            </button>

            <button
              onClick={handleImportClick}
              className="py-3 px-5 rounded-2xl bg-orange-600 hover:bg-orange-500 text-white font-semibold text-xs shadow-lg shadow-orange-950/40 flex items-center justify-center gap-2 transition-all cursor-pointer"
            >
              <FolderPlus className="w-4 h-4" />
              <span>Import Codebase</span>
              <span className="text-[10px] font-mono opacity-70">
                ({codebases.length}/{MAX_CODEBASES})
              </span>
            </button>
          </div>
        </header>

        {/* Dashboard Stats Overview */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="p-5 rounded-3xl bg-[#0F141E]/90 border border-slate-800/80 space-y-2 relative overflow-hidden">
            <div className="w-8 h-8 rounded-xl bg-orange-500/10 border border-orange-500/20 text-orange-400 flex items-center justify-center">
              <Layers className="w-4 h-4" />
            </div>
            <h3 className="text-xs font-semibold text-slate-400">
              Total Codebases
            </h3>
            <p className="text-2xl font-extrabold text-white tracking-tight">
              {codebases.length}
              <span className="text-xs font-normal text-slate-500 ml-1">
                / {MAX_CODEBASES} max
              </span>
            </p>
          </div>

          <div className="p-5 rounded-3xl bg-[#0F141E]/90 border border-slate-800/80 space-y-2 relative overflow-hidden">
            <div className="w-8 h-8 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 flex items-center justify-center">
              <Sparkles className="w-4 h-4" />
            </div>
            <h3 className="text-xs font-semibold text-slate-400">
              Indexed Repositories
            </h3>
            <p className="text-2xl font-extrabold text-emerald-400 tracking-tight">
              {indexedReposCount}{' '}
              <span className="text-xs font-normal text-slate-500">
                / {codebases.length}
              </span>
            </p>
          </div>

          <div className="p-5 rounded-3xl bg-[#0F141E]/90 border border-slate-800/80 space-y-2 relative overflow-hidden">
            <div className="w-8 h-8 rounded-xl bg-cyan-500/10 border border-cyan-500/20 text-cyan-400 flex items-center justify-center">
              <FileCode className="w-4 h-4" />
            </div>
            <h3 className="text-xs font-semibold text-slate-400">
              Indexed Source Files
            </h3>
            <p className="text-2xl font-extrabold text-cyan-400 tracking-tight">
              {totalIndexedFiles}
            </p>
          </div>
        </div>

        {/* Search & Actions Bar */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-2">
          <div className="relative w-full sm:w-80">
            <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" />
            <input
              type="text"
              placeholder="Search codebases by name, branch..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 rounded-2xl bg-[#0F141E]/90 border border-slate-800/80 text-slate-100 text-xs focus:outline-none focus:border-orange-500/50 transition-all placeholder:text-slate-500"
            />
          </div>

          <p className="text-xs text-slate-400 self-end sm:self-center">
            Showing{' '}
            <span className="text-slate-200 font-bold">
              {filteredCodebases.length}
            </span>{' '}
            of{' '}
            <span className="text-slate-200 font-bold font-mono">
              {codebases.length}
            </span>{' '}
            repositories
          </p>
        </div>

        {/* Loading State */}
        {isLoading && (
          <div className="flex items-center justify-center py-20">
            <div className="flex items-center gap-3 text-slate-400">
              <Loader2 className="w-6 h-6 animate-spin text-orange-400" />
              <span className="text-sm font-semibold">
                Loading codebases...
              </span>
            </div>
          </div>
        )}

        {/* Error State */}
        {isError && !isLoading && (
          <div className="p-8 rounded-3xl bg-red-500/5 border border-red-500/20 text-center space-y-4">
            <div className="w-12 h-12 rounded-2xl bg-red-500/10 border border-red-500/20 text-red-400 flex items-center justify-center mx-auto">
              <AlertCircle className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-white">
              Failed to load codebases
            </h3>
            <p className="text-xs text-slate-400 max-w-sm mx-auto">
              {queryError instanceof Error
                ? queryError.message
                : 'An unexpected error occurred while fetching your codebases.'}
            </p>
            <button
              onClick={() => refetch()}
              className="py-2.5 px-5 rounded-xl bg-red-600/80 hover:bg-red-500 text-white font-semibold text-xs transition-all cursor-pointer inline-flex items-center gap-2"
            >
              <RefreshCw className="w-4 h-4" />
              <span>Try Again</span>
            </button>
          </div>
        )}

        {/* Codebases Grid */}
        {!isLoading && !isError && (
          <>
            {filteredCodebases.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {filteredCodebases.map((codebase) => (
                  <CodebaseCard
                    key={codebase.codebaseId}
                    codebase={codebase}
                    onDelete={handleDeleteCodebase}
                    onReindex={handleReindexCodebase}
                    onEdit={setEditingCodebase}
                    isDeleting={
                      deleteMutation.isPending &&
                      deleteMutation.variables === codebase.codebaseId
                    }
                    isReindexing={
                      reindexMutation.isPending &&
                      reindexMutation.variables === codebase.codebaseId
                    }
                  />
                ))}
              </div>
            ) : (
              <div className="p-12 rounded-3xl bg-[#0F141E]/60 border border-slate-800/80 text-center space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-slate-900 border border-slate-800 text-slate-500 flex items-center justify-center mx-auto">
                  <FileCode className="w-6 h-6" />
                </div>
                <h3 className="text-base font-bold text-white">
                  No codebases found
                </h3>
                <p className="text-xs text-slate-400 max-w-sm mx-auto">
                  {searchQuery
                    ? 'No codebase matches your search query. Try clearing filters.'
                    : 'You have not imported any repositories yet. Click below to queue your first codebase import.'}
                </p>
                <button
                  onClick={handleImportClick}
                  className="py-2.5 px-5 rounded-xl bg-orange-600 hover:bg-orange-500 text-white font-semibold text-xs transition-all cursor-pointer inline-flex items-center gap-2"
                >
                  <FolderPlus className="w-4 h-4" />
                  <span>Import First Codebase</span>
                </button>
              </div>
            )}
          </>
        )}
      </div>

      {/* Import Modal */}
      <CodebaseImportDialog
        isOpen={isImportModalOpen}
        onClose={() => setIsImportModalOpen(false)}
        onSuccess={() => {
          setIsImportModalOpen(false)
        }}
      />

      {/* Edit Modal */}
      {editingCodebase && (
        <CodebaseEditDialog
          isOpen={!!editingCodebase}
          codebase={editingCodebase}
          onClose={() => setEditingCodebase(null)}
        />
      )}

      {/* Delete Confirmation Modal */}
      {deletingCodebase && (
        <CodebaseDeleteDialog
          isOpen={!!deletingCodebase}
          codebase={deletingCodebase}
          onClose={() => setDeletingCodebase(null)}
          onConfirm={handleConfirmDelete}
          isDeleting={deleteMutation.isPending}
        />
      )}
    </div>
  )
}
