import type { ReactNode } from 'react'
import { Card, CardContent } from '@/components/ui/card'

interface EmptyStateProps {
  icon?: ReactNode
  title: string
  description: string
  action?: ReactNode
}

export function EmptyState({ icon, title, description, action }: EmptyStateProps) {
  return (
    <Card className="border-dashed border-zinc-800 bg-zinc-950/40">
      <CardContent className="flex flex-col items-center justify-center p-12 text-center">
        {icon && <div className="mb-4 text-zinc-500">{icon}</div>}
        <h3 className="text-lg font-medium text-zinc-100">{title}</h3>
        <p className="mt-1 max-w-md text-sm text-zinc-400">{description}</p>
        {action && <div className="mt-6">{action}</div>}
      </CardContent>
    </Card>
  )
}
