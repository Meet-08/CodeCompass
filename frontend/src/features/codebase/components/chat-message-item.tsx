import { Bot, User as UserIcon, BookOpen, Copy, Check } from 'lucide-react'
import { useState } from 'react'
import type { CodeCitation } from '#/features/chat'
import { CitationBadge } from './citation-badge'

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  citations?: CodeCitation[]
  isStreaming?: boolean
}

interface ChatMessageItemProps {
  message: ChatMessage
}

export function ChatMessageItem({ message }: ChatMessageItemProps) {
  const isUser = message.role === 'user'

  return (
    <div
      className={`flex gap-4 ${isUser ? 'justify-end' : 'justify-start'} group animate-in fade-in duration-200`}
    >
      {/* Bot Avatar */}
      {!isUser && (
        <div className="w-9 h-9 rounded-2xl bg-cyan-500/10 border border-cyan-500/20 text-cyan-400 flex items-center justify-center shrink-0 mt-1">
          <Bot className="w-5 h-5" />
        </div>
      )}

      {/* Message Content Container */}
      <div
        className={`space-y-3 max-w-3xl ${isUser ? 'items-end' : 'items-start'}`}
      >
        <div
          className={`p-4 sm:p-5 rounded-3xl text-sm leading-relaxed ${
            isUser
              ? 'bg-gradient-to-r from-cyan-600 to-blue-600 border border-cyan-500/30 text-white rounded-tr-sm shadow-md shadow-cyan-950/20 font-medium'
              : 'bg-[#0F141E] border border-slate-800 text-slate-200 rounded-tl-sm shadow-xl'
          }`}
        >
          {/* Formatted Content */}
          {isUser ? (
            <div className="whitespace-pre-wrap font-sans text-xs sm:text-sm">
              {message.content}
            </div>
          ) : (
            <FormattedMarkdown content={message.content} />
          )}

          {/* Streaming cursor */}
          {message.isStreaming && (
            <span className="inline-flex items-center gap-0.5 ml-1 align-middle">
              <span className="w-1.5 h-4 bg-cyan-400 rounded-sm animate-pulse" />
            </span>
          )}
        </div>

        {/* Citations Drawer if available */}
        {!isUser && message.citations && message.citations.length > 0 && (
          <div className="p-3.5 rounded-2xl bg-[#080B11]/80 border border-slate-800/80 space-y-2">
            <div className="flex items-center gap-2 text-xs font-semibold text-slate-400 border-b border-slate-800/80 pb-2">
              <BookOpen className="w-3.5 h-3.5 text-cyan-400" />
              <span>Source Code Citations ({message.citations.length})</span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1">
              {message.citations.map((citation, index) => (
                <CitationBadge
                  key={citation.chunkId || index}
                  citation={citation}
                />
              ))}
            </div>
          </div>
        )}
      </div>

      {/* User Avatar */}
      {isUser && (
        <div className="w-9 h-9 rounded-2xl bg-cyan-950/60 border border-cyan-800/50 text-cyan-300 flex items-center justify-center shrink-0 mt-1 shadow-sm">
          <UserIcon className="w-4 h-4" />
        </div>
      )}
    </div>
  )
}

function FormattedMarkdown({ content }: { content: string }) {
  // Split content by fenced code blocks
  const parts = content.split(/(```[\s\S]*?```)/g)

  return (
    <div className="space-y-3 font-sans text-xs sm:text-sm leading-relaxed">
      {parts.map((part, i) => {
        if (part.startsWith('```') && part.endsWith('```')) {
          const firstLineEnd = part.indexOf('\n')
          const lang =
            firstLineEnd !== -1 ? part.slice(3, firstLineEnd).trim() : ''
          const codeSnippet =
            firstLineEnd !== -1
              ? part.slice(firstLineEnd + 1, -3)
              : part.slice(3, -3)

          return <CodeBlock key={i} lang={lang} code={codeSnippet} />
        }

        const lines = part.split('\n')
        return (
          <div key={i} className="space-y-1.5">
            {lines.map((line, lineIdx) => {
              if (!line.trim()) {
                return <div key={lineIdx} className="h-1.5" />
              }

              // Headings #, ##, ###
              if (line.startsWith('#')) {
                const headingLevel = line.match(/^#+/)?.[0].length || 1
                const text = line.replace(/^#+\s*/, '')
                const headingClasses =
                  headingLevel === 1
                    ? 'text-base font-extrabold text-white mt-3 mb-1'
                    : headingLevel === 2
                      ? 'text-sm font-extrabold text-white mt-2.5 mb-1'
                      : 'text-xs font-bold text-cyan-400 mt-2 mb-1'
                return (
                  <div key={lineIdx} className={headingClasses}>
                    {renderInlineFormatted(text)}
                  </div>
                )
              }

              // Lists
              if (/^(\d+\.|\*|-)\s+/.test(line.trim())) {
                return (
                  <div
                    key={lineIdx}
                    className="pl-3.5 border-l-2 border-cyan-500/40 text-slate-200 my-1 font-sans"
                  >
                    {renderInlineFormatted(line)}
                  </div>
                )
              }

              return (
                <p key={lineIdx} className="text-slate-200 leading-relaxed">
                  {renderInlineFormatted(line)}
                </p>
              )
            })}
          </div>
        )
      })}
    </div>
  )
}

function CodeBlock({ lang, code }: { lang: string; code: string }) {
  const [copied, setCopied] = useState(false)

  const handleCopy = () => {
    navigator.clipboard.writeText(code)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="my-3 rounded-2xl bg-[#080B11] border border-slate-800/90 overflow-hidden shadow-lg">
      <div className="px-4 py-2 bg-slate-900/90 border-b border-slate-800/80 text-[10px] font-mono text-cyan-400 font-semibold uppercase tracking-wider flex items-center justify-between">
        <span>{lang || 'code'}</span>
        <button
          onClick={handleCopy}
          className="flex items-center gap-1 text-slate-400 hover:text-white transition-colors cursor-pointer"
        >
          {copied ? (
            <>
              <Check className="w-3 h-3 text-emerald-400" />
              <span className="text-emerald-400">Copied</span>
            </>
          ) : (
            <>
              <Copy className="w-3 h-3" />
              <span>Copy</span>
            </>
          )}
        </button>
      </div>
      <pre className="p-4 overflow-x-auto font-mono text-[11px] leading-relaxed text-slate-200 scrollbar-thin scrollbar-thumb-slate-800">
        <code>{code}</code>
      </pre>
    </div>
  )
}

function renderInlineFormatted(text: string) {
  const tokens = text.split(/(\*\*.*?\*\*|`.*?`)/g)
  return tokens.map((token, idx) => {
    if (token.startsWith('**') && token.endsWith('**')) {
      return (
        <strong key={idx} className="font-bold text-white">
          {token.slice(2, -2)}
        </strong>
      )
    }
    if (token.startsWith('`') && token.endsWith('`')) {
      return (
        <code
          key={idx}
          className="px-1.5 py-0.5 rounded bg-slate-900 border border-slate-800 font-mono text-cyan-300 text-[11px]"
        >
          {token.slice(1, -1)}
        </code>
      )
    }
    return token
  })
}
