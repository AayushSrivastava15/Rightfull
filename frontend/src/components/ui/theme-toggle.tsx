import React from 'react'
import { Sun, Moon, Laptop, Check } from 'lucide-react'
import { useTheme, type Theme } from '@/contexts/ThemeContext'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Button } from '@/components/ui/button'
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip'

export function ThemeToggle({
  className,
  defaultOpen,
}: {
  className?: string | undefined
  defaultOpen?: boolean | undefined
}) {
  const { theme, resolvedTheme, setTheme } = useTheme()

  const items: { label: string; value: Theme; icon: React.ReactNode }[] = [
    { label: 'Light', value: 'light', icon: <Sun className="h-4 w-4" /> },
    { label: 'Dark', value: 'dark', icon: <Moon className="h-4 w-4" /> },
    { label: 'System', value: 'system', icon: <Laptop className="h-4 w-4" /> },
  ]

  return (
    <TooltipProvider>
      <DropdownMenu defaultOpen={defaultOpen}>
        <Tooltip>
          <TooltipTrigger asChild>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                size="icon"
                className={`relative h-9 w-9 rounded-lg border border-border/50 bg-background/50 text-foreground transition-all duration-200 hover:bg-accent hover:text-accent-foreground ${className ?? ''}`}
                aria-label={`Toggle theme (currently ${theme})`}
              >
                <Sun className="h-4 w-4 rotate-0 scale-100 transition-all dark:-rotate-90 dark:scale-0" />
                <Moon className="absolute h-4 w-4 rotate-90 scale-0 transition-all dark:rotate-0 dark:scale-100" />
                <span className="sr-only">Toggle theme</span>
              </Button>
            </DropdownMenuTrigger>
          </TooltipTrigger>
          <TooltipContent side="bottom" align="center" className="text-xs">
            Switch theme ({theme})
          </TooltipContent>
        </Tooltip>

        <DropdownMenuContent align="end" className="w-36 p-1">
          {items.map((item) => (
            <DropdownMenuItem
              key={item.value}
              onClick={() => setTheme(item.value)}
              className="flex items-center justify-between text-xs cursor-pointer py-1.5"
            >
              <div className="flex items-center gap-2">
                {item.icon}
                <span>{item.label}</span>
              </div>
              {theme === item.value && <Check className="h-3.5 w-3.5 text-primary" />}
            </DropdownMenuItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>
    </TooltipProvider>
  )
}

