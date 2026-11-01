import React, { useState } from 'react'
import { Check, Copy, Terminal } from 'lucide-react'
import { cn } from '@/lib/utils'

interface TerminalBlockProps {
  title?: string
  code: string
  language?: string
  className?: string
}

export function TerminalBlock({
  title = 'bash',
  code,
  language = 'bash',
  className,
}: TerminalBlockProps) {
  const [copied, setCopied] = useState(false)

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(code)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch (e) {
      console.warn('Copy to clipboard failed:', e)
    }
  }

  return (
    <div
      className={cn(
        'relative overflow-hidden rounded-xl border border-border/60 bg-zinc-950 text-zinc-100 font-mono text-xs shadow-md dark:border-border/40',
        className
      )}
    >
      {/* Terminal Title Bar */}
      <div className="flex h-9 items-center justify-between border-b border-zinc-800/80 bg-zinc-900/60 px-4 text-zinc-400">
        <div className="flex items-center gap-2">
          <div className="flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 rounded-full bg-red-500/80" />
            <span className="h-2.5 w-2.5 rounded-full bg-amber-500/80" />
            <span className="h-2.5 w-2.5 rounded-full bg-emerald-500/80" />
          </div>
          <span className="ml-2 text-[11px] font-medium text-zinc-400 flex items-center gap-1.5">
            <Terminal className="h-3 w-3 text-zinc-500" />
            {title}
          </span>
        </div>

        <div className="flex items-center gap-3">
          <span className="text-[10px] uppercase tracking-wider text-zinc-500">
            {language}
          </span>
          <button
            onClick={handleCopy}
            className="flex items-center gap-1 rounded px-1.5 py-0.5 text-[11px] text-zinc-400 transition-colors hover:bg-zinc-800 hover:text-zinc-200"
            title="Copy code"
            aria-label="Copy code to clipboard"
          >
            {copied ? (
              <>
                <Check className="h-3 w-3 text-emerald-400" />
                <span className="text-emerald-400">Copied</span>
              </>
            ) : (
              <>
                <Copy className="h-3 w-3" />
                <span>Copy</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* Code Area */}
      <div className="overflow-x-auto p-4 leading-relaxed">
        <pre className="text-zinc-200 whitespace-pre">
          <code>{code}</code>
        </pre>
      </div>
    </div>
  )
}

