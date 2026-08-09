import { useState, useRef, useEffect } from 'react'
import {
  Send,
  Square,
  AlertCircle,
  Bot,
  RefreshCw,
  Loader2,
} from 'lucide-react'
import { useChatStream, useChatHistory } from '#/features/chat'
import type { CodeCitation } from '#/features/chat'
import { ChatMessageItem } from './chat-message-item'
import type { ChatMessage } from './chat-message-item'

interface CodebaseChatContainerProps {
  codebaseId: string
  codebaseName?: string
  sessionId?: string | null
  onSessionResolved?: (sessionId: string) => void
  onTitleResolved?: (title: string) => void
}

export function CodebaseChatContainer({
  codebaseId,
  codebaseName,
  sessionId,
  onSessionResolved,
  onTitleResolved,
}: CodebaseChatContainerProps) {
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([
    {
      id: 'welcome',
      role: 'assistant',
      content: `Hello! I am your AI assistant for ${codebaseName || 'this repository'}. Ask me anything about the codebase structure, classes, API routes, or specific logic!`,
    },
  ])
  const [inputPrompt, setInputPrompt] = useState('')
  const [currentSessionId, setCurrentSessionId] = useState<string | null>(
    sessionId ?? null,
  )
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const citationsCacheRef = useRef<Map<string, CodeCitation[]>>(new Map())

  const {
    messages: streamingContent,
    citations: streamingCitations,
    streamTitle,
    isStreaming,
    error,
    chatId: resolvedChatId,
    sendMessage,
    abort,
  } = useChatStream(codebaseId)

  // Fetch persisted message history when a session is selected
  const { data: historyResponse, isFetching: isLoadingHistory } =
    useChatHistory(codebaseId, sessionId)

  // Scroll to bottom when messages update or streaming changes
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [chatMessages, streamingContent, isStreaming])

  // Reset chat messages when sessionId changes (session switch)
  const prevSessionIdRef = useRef(sessionId)
  useEffect(() => {
    if (prevSessionIdRef.current !== sessionId) {
      // If this session change is simply resolving a newly created session locally, preserve local messages!
      if (
        currentSessionId &&
        sessionId === currentSessionId &&
        prevSessionIdRef.current === null
      ) {
        prevSessionIdRef.current = sessionId
        return
      }

      abort()
      setChatMessages([
        {
          id: 'welcome',
          role: 'assistant',
          content: sessionId
            ? `Loading conversation history…`
            : `Hello! I am your AI assistant for ${codebaseName || 'this repository'}. Ask me anything about the codebase structure, classes, API routes, or specific logic!`,
        },
      ])
      setCurrentSessionId(sessionId ?? null)
      prevSessionIdRef.current = sessionId
    }
  }, [sessionId, codebaseName, abort, currentSessionId])

  // Hydrate chat messages from fetched history
  const hydratedSessionRef = useRef<string | null>(null)
  useEffect(() => {
    if (
      sessionId &&
      historyResponse?.success &&
      historyResponse.data?.messages &&
      hydratedSessionRef.current !== sessionId
    ) {
      hydratedSessionRef.current = sessionId
      const historicMessages: ChatMessage[] = historyResponse.data.messages.map(
        (msg) => {
          const cachedCitations = citationsCacheRef.current.get(msg.messageId)
          return {
            id: msg.messageId,
            role: msg.role === 'USER' ? 'user' : 'assistant',
            content: msg.content,
            citations: cachedCitations,
          }
        },
      )
      if (historicMessages.length > 0) {
        setChatMessages(historicMessages)
      } else {
        setChatMessages([
          {
            id: 'welcome',
            role: 'assistant',
            content: `Continuing conversation for ${codebaseName || 'this repository'}. Send a message to continue.`,
          },
        ])
      }
    }
    // Reset hydration tracker when session is cleared
    if (!sessionId) {
      hydratedSessionRef.current = null
    }
  }, [sessionId, historyResponse, codebaseName])

  // Sync completed stream to message history & save citations cache
  const prevStreamingRef = useRef(isStreaming)
  useEffect(() => {
    if (prevStreamingRef.current && !isStreaming) {
      if (streamingContent.trim()) {
        const msgId = `assistant-${Date.now()}`
        if (streamingCitations && streamingCitations.length > 0) {
          citationsCacheRef.current.set(msgId, streamingCitations)
        }
        setChatMessages((prev) => [
          ...prev,
          {
            id: msgId,
            role: 'assistant',
            content: streamingContent,
            citations: streamingCitations,
          },
        ])
      }
    }
    prevStreamingRef.current = isStreaming
  }, [isStreaming, streamingContent, streamingCitations])

  // Capture resolved chatId from the stream done event
  useEffect(() => {
    if (resolvedChatId) {
      setCurrentSessionId(resolvedChatId)
      onSessionResolved?.(resolvedChatId)
    }
  }, [resolvedChatId, onSessionResolved])

  // Capture resolved title from the stream
  useEffect(() => {
    if (streamTitle) {
      onTitleResolved?.(streamTitle)
    }
  }, [streamTitle, onTitleResolved])

  const handleSend = (textToSend?: string) => {
    const prompt = (textToSend || inputPrompt).trim()
    if (!prompt || isStreaming) return

    // Push user message
    const userMsg: ChatMessage = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: prompt,
    }

    setChatMessages((prev) => [...prev, userMsg])
    setInputPrompt('')

    sendMessage({
      chatId: currentSessionId || undefined,
      message: prompt,
    })
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const handleClearChat = () => {
    abort()
    setCurrentSessionId(null)
    setChatMessages([
      {
        id: 'welcome',
        role: 'assistant',
        content: `Conversation reset. Ask me anything about ${codebaseName || 'this repository'}!`,
      },
    ])
  }

  return (
    <div className="flex flex-col h-full bg-[#080B11] text-slate-100 rounded-3xl border border-slate-800/80 overflow-hidden shadow-2xl relative">
      {/* Header Bar */}
      <div className="p-4 sm:px-6 bg-[#0F141E]/90 border-b border-slate-800/80 flex items-center justify-between z-10">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-xl bg-cyan-500/10 border border-cyan-500/20 text-cyan-400 flex items-center justify-center">
            <Bot className="w-4 h-4" />
          </div>
          <div>
            <h2 className="text-sm font-bold text-white flex items-center gap-2">
              Code Intelligence Assistant
              <span className="inline-block w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
            </h2>
            <p className="text-[11px] text-slate-400 font-mono truncate max-w-xs sm:max-w-md">
              {streamTitle || codebaseName || codebaseId}
            </p>
          </div>
        </div>

        <button
          onClick={handleClearChat}
          className="px-3 py-1.5 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-400 hover:text-slate-200 text-xs font-semibold flex items-center gap-1.5 transition-all cursor-pointer"
          title="Start a new conversation"
        >
          <RefreshCw className="w-3.5 h-3.5 text-cyan-400" />
          <span className="hidden sm:inline">New Chat</span>
        </button>
      </div>

      {/* Messages Scroll Area */}
      <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-6 scrollbar-thin scrollbar-thumb-slate-800">
        {chatMessages.map((msg) => (
          <ChatMessageItem key={msg.id} message={msg} />
        ))}

        {/* History Loading Indicator (only show during initial load) */}
        {isLoadingHistory && sessionId && chatMessages.length <= 1 && (
          <div className="flex items-center justify-center py-8 gap-2 text-slate-500">
            <Loader2 className="w-4 h-4 animate-spin text-cyan-400" />
            <span className="text-xs">Loading conversation history…</span>
          </div>
        )}

        {/* Active Streaming Message */}
        {isStreaming && (
          <ChatMessageItem
            message={{
              id: 'streaming-active',
              role: 'assistant',
              content:
                streamingContent ||
                'Analyzing codebase and generating answer...',
              citations: streamingCitations,
              isStreaming: true,
            }}
          />
        )}

        {/* Error Notification Banner */}
        {error && (
          <div className="p-4 rounded-2xl bg-red-500/10 border border-red-500/20 text-red-400 text-xs flex items-center gap-3">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>Stream error: {error}</span>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Input Form Footer */}
      <div className="p-4 sm:p-5 bg-[#0F141E]/90 border-t border-slate-800/80 z-10">
        <form
          onSubmit={(e) => {
            e.preventDefault()
            handleSend()
          }}
          className="flex items-end gap-3 bg-slate-900/90 border border-slate-800 rounded-2xl p-2.5 focus-within:border-cyan-500/50 focus-within:ring-1 focus-within:ring-cyan-500/20 transition-all"
        >
          <textarea
            value={inputPrompt}
            onChange={(e) => setInputPrompt(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ask a question about this repository... (Enter to send, Shift+Enter for newline)"
            rows={1}
            className="flex-1 bg-transparent border-0 text-slate-100 text-xs sm:text-sm placeholder:text-slate-500 focus:outline-none resize-none max-h-32 p-1 font-sans"
          />

          {isStreaming ? (
            <button
              type="button"
              onClick={abort}
              className="py-2 px-3.5 rounded-xl bg-red-600/90 hover:bg-red-500 text-white font-semibold text-xs flex items-center gap-1.5 transition-all cursor-pointer shrink-0 shadow-md shadow-red-950/30"
            >
              <Square className="w-3.5 h-3.5 fill-current" />
              <span>Stop</span>
            </button>
          ) : (
            <button
              type="submit"
              disabled={!inputPrompt.trim()}
              className="py-2 px-4 rounded-xl bg-gradient-to-r from-cyan-600 to-blue-600 hover:from-cyan-500 hover:to-blue-500 text-white font-semibold text-xs flex items-center gap-1.5 transition-all cursor-pointer shrink-0 disabled:opacity-40 disabled:cursor-not-allowed shadow-md shadow-cyan-950/30"
            >
              <span>Send</span>
              <Send className="w-3.5 h-3.5" />
            </button>
          )}
        </form>
      </div>
    </div>
  )
}
