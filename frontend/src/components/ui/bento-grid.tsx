import React from 'react'
import { ArrowRight } from 'lucide-react'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'

export function BentoGrid({
  children,
  className,
}: {
  children: React.ReactNode
  className?: string
}) {
  return (
    <div
      className={cn(
        'grid w-full auto-rows-[22rem] grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3',
        className
      )}
    >
      {children}
    </div>
  )
}

export function BentoCard({
  name,
  className,
  background,
  Icon,
  description,
  href,
  cta,
  children,
  onClick,
}: {
  name: string
  className?: string
  background?: React.ReactNode
  Icon?: React.ComponentType<{ className?: string }>
  description: string
  href?: string
  cta?: string
  children?: React.ReactNode
  onClick?: () => void
}) {
  return (
    <div
      key={name}
      onClick={onClick}
      className={cn(
        'group relative col-span-1 flex flex-col justify-between overflow-hidden rounded-xl border border-border/60 bg-card p-6 shadow-sm transition-all duration-300 hover:border-border hover:shadow-md dark:border-border/40',
        onClick && 'cursor-pointer',
        className
      )}
    >
      {background && (
        <div className="absolute inset-0 z-0 overflow-hidden pointer-events-none opacity-40 transition-opacity duration-300 group-hover:opacity-60">
          {background}
        </div>
      )}

      {/* Top Header / Content */}
      <div className="relative z-10 flex flex-col gap-2">
        {Icon && (
          <div className="flex h-10 w-10 items-center justify-center rounded-lg border border-border/60 bg-secondary/50 text-foreground transition-colors group-hover:border-primary/40 group-hover:text-primary">
            <Icon className="h-5 w-5" />
          </div>
        )}
        <div className="mt-2">
          <h3 className="text-base font-semibold tracking-tight text-foreground">
            {name}
          </h3>
          <p className="mt-1 text-xs text-muted-foreground line-clamp-2">
            {description}
          </p>
        </div>
      </div>

      {/* Center Body (if any) */}
      {children && <div className="relative z-10 my-auto py-2">{children}</div>}

      {/* Bottom Action Footer */}
      {(cta || href) && (
        <div className="relative z-10 mt-auto flex items-center justify-between pt-4 border-t border-border/40">
          <span className="text-xs font-medium text-primary flex items-center gap-1 group-hover:translate-x-0.5 transition-transform">
            {cta || 'Explore'}
            <ArrowRight className="h-3.5 w-3.5" />
          </span>
        </div>
      )}
    </div>
  )
}

