import { useState, useRef, useEffect } from 'react'
import {
  Send,
  Square,
  Sparkles,
  AlertCircle,
  Bot,
  RefreshCw,
} from 'lucide-react'
import { useChatStream } from '../hooks/use-codebase'
import { ChatMessageItem  } from './chat-message-item'
import type {ChatMessage} from './chat-message-item';

interface CodebaseChatContainerProps {
  codebaseId: string
  codebaseName?: string
}

export function CodebaseChatContainer({
  codebaseId,
  codebaseName,
}: CodebaseChatContainerProps) {
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([
    {
      id: 'welcome',
      role: 'assistant',
      content: `Hello! I am your AI assistant for ${codebaseName || 'this repository'}. Ask me anything about the codebase structure, classes, API routes, or specific logic!`,
    },
  ])
  const [inputPrompt, setInputPrompt] = useState('')
  const messagesEndRef = useRef<HTMLDivElement>(null)

  const {
    messages: streamingContent,
    citations: streamingCitations,
    isStreaming,
    error,
    sendMessage,
    abort,
  } = useChatStream(codebaseId)

  // Scroll to bottom when messages update or streaming changes
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [chatMessages, streamingContent, isStreaming])

  // Sync completed stream to message history
  const prevStreamingRef = useRef(isStreaming)
  useEffect(() => {
    if (prevStreamingRef.current && !isStreaming) {
      if (streamingContent.trim()) {
        setChatMessages((prev) => [
          ...prev,
          {
            id: `assistant-${Date.now()}`,
            role: 'assistant',
            content: streamingContent,
            citations: streamingCitations,
          },
        ])
      }
    }
    prevStreamingRef.current = isStreaming
  }, [isStreaming, streamingContent, streamingCitations])

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

    // Trigger API SSE stream
    sendMessage({
      chatId: 'default',
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
    setChatMessages([
      {
        id: 'welcome',
        role: 'assistant',
        content: `Conversation reset. Ask me anything about ${codebaseName || 'this repository'}!`,
      },
    ])
  }

  const samplePrompts = [
    'How does repository clone and indexing work?',
    'What API endpoints are available in this codebase?',
    'Explain the security filter and JWT auth setup.',
  ]

  return (
    <div className="flex flex-col h-[calc(100vh-140px)] bg-[#080B11] text-slate-100 rounded-3xl border border-slate-800/80 overflow-hidden shadow-2xl relative">
      {/* Background Ambient Glow */}
      <div className="absolute top-0 right-0 w-[40%] h-[30%] bg-orange-500/5 blur-[120px] pointer-events-none" />

      {/* Header Bar */}
      <div className="p-4 sm:px-6 bg-[#0F141E]/90 border-b border-slate-800/80 flex items-center justify-between z-10">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-xl bg-orange-500/10 border border-orange-500/20 text-orange-400 flex items-center justify-center">
            <Bot className="w-4 h-4" />
          </div>
          <div>
            <h2 className="text-sm font-bold text-white flex items-center gap-2">
              Code Intelligence Assistant
              <span className="inline-block w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
            </h2>
            <p className="text-[11px] text-slate-400 font-mono truncate max-w-xs sm:max-w-md">
              Target: {codebaseName || codebaseId}
            </p>
          </div>
        </div>

        <button
          onClick={handleClearChat}
          className="px-3 py-1.5 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-400 hover:text-slate-200 text-xs font-semibold flex items-center gap-1.5 transition-all cursor-pointer"
          title="Reset conversation"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          <span className="hidden sm:inline">Reset Chat</span>
        </button>
      </div>

      {/* Messages Scroll Area */}
      <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-6 scrollbar-thin scrollbar-thumb-slate-800">
        {chatMessages.map((msg) => (
          <ChatMessageItem key={msg.id} message={msg} />
        ))}

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

      {/* Quick Prompt Chips (Visible when list has only welcome message) */}
      {chatMessages.length <= 1 && !isStreaming && (
        <div className="px-4 sm:px-6 py-2 flex flex-wrap gap-2 z-10">
          {samplePrompts.map((promptText, idx) => (
            <button
              key={idx}
              onClick={() => handleSend(promptText)}
              className="px-3 py-1.5 rounded-xl bg-slate-900/80 hover:bg-slate-800 border border-slate-800 text-slate-300 text-xs flex items-center gap-1.5 transition-all cursor-pointer hover:border-orange-500/30"
            >
              <Sparkles className="w-3 h-3 text-orange-400" />
              <span>{promptText}</span>
            </button>
          ))}
        </div>
      )}

      {/* Input Form Footer */}
      <div className="p-4 sm:p-5 bg-[#0F141E]/90 border-t border-slate-800/80 z-10">
        <form
          onSubmit={(e) => {
            e.preventDefault()
            handleSend()
          }}
          className="flex items-end gap-3 bg-slate-900/90 border border-slate-800 rounded-2xl p-2.5 focus-within:border-orange-500/50 transition-all"
        >
          <textarea
            value={inputPrompt}
            onChange={(e) => setInputPrompt(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ask a question about this repository... (Press Enter to send, Shift+Enter for newline)"
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
              className="py-2 px-4 rounded-xl bg-orange-600 hover:bg-orange-500 text-white font-semibold text-xs flex items-center gap-1.5 transition-all cursor-pointer shrink-0 disabled:opacity-40 disabled:cursor-not-allowed shadow-md shadow-orange-950/30"
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
