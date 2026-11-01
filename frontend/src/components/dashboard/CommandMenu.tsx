import React, { useEffect, useState } from 'react'
import {
  LayoutDashboard,
  Package,
  Sliders,
  Users,
  Code2,
  Settings,
  Sun,
  Moon,
  Laptop,
  Copy,
  LogOut,
  ShieldCheck,
  Search,
  KeyRound,
  Cpu,
} from 'lucide-react'
import {
  CommandDialog,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
  CommandSeparator,
} from '@/components/ui/command'
import { useTheme } from '@/contexts/ThemeContext'
import { useAuth } from '@/contexts/AuthContext'

interface CommandMenuProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onNavigate: (tab: any) => void
}

export function CommandMenu({ open, onOpenChange, onNavigate }: CommandMenuProps) {
  const { setTheme } = useTheme()
  const { profile, signOut } = useAuth()
  const [copiedWs, setCopiedWs] = useState(false)

  const handleSelect = (action: () => void) => {
    action()
    onOpenChange(false)
  }

  const copyWorkspaceId = async () => {
    if (profile?.defaultWorkspace?.id) {
      await navigator.clipboard.writeText(profile.defaultWorkspace.id)
      setCopiedWs(true)
      setTimeout(() => setCopiedWs(false), 2000)
    }
  }

  return (
    <CommandDialog open={open} onOpenChange={onOpenChange}>
      <CommandInput placeholder="Type a command or search..." />
      <CommandList>
        <CommandEmpty>No results found.</CommandEmpty>

        <CommandGroup heading="Navigation">
          <CommandItem
            onSelect={() => handleSelect(() => onNavigate('overview'))}
            className="flex items-center gap-2 cursor-pointer"
          >
            <LayoutDashboard className="h-4 w-4 text-primary" />
            <span>Overview</span>
          </CommandItem>
          <CommandItem
            onSelect={() => handleSelect(() => onNavigate('products'))}
            className="flex items-center gap-2 cursor-pointer"
          >
            <Package className="h-4 w-4 text-blue-500" />
            <span>Products & Signing Keys</span>
          </CommandItem>
          <CommandItem
            onSelect={() => handleSelect(() => onNavigate('configurations'))}
            className="flex items-center gap-2 cursor-pointer"
          >
            <Sliders className="h-4 w-4 text-violet-500" />
            <span>License Configurations</span>
          </CommandItem>
          <CommandItem
            onSelect={() => handleSelect(() => onNavigate('licenses'))}
            className="flex items-center gap-2 cursor-pointer"
          >
            <KeyRound className="h-4 w-4 text-pink-500" />
            <span>Issued Licenses & Keys</span>
          </CommandItem>
          <CommandItem
            onSelect={() => handleSelect(() => onNavigate('activations'))}
            className="flex items-center gap-2 cursor-pointer"
          >
            <Cpu className="h-4 w-4 text-cyan-500" />
            <span>Device & Node Activations</span>
          </CommandItem>
          <CommandItem
            onSelect={() => handleSelect(() => onNavigate('customers'))}
            className="flex items-center gap-2 cursor-pointer"
          >
            <Users className="h-4 w-4 text-emerald-500" />
            <span>Customer Directory</span>
          </CommandItem>
          <CommandItem
            onSelect={() => handleSelect(() => onNavigate('developer'))}
            className="flex items-center gap-2 cursor-pointer"
          >
            <Code2 className="h-4 w-4 text-amber-500" />
            <span>Developer & API Documentation</span>
          </CommandItem>
          <CommandItem
            onSelect={() => handleSelect(() => onNavigate('team'))}
            className="flex items-center gap-2 cursor-pointer"
          >
            <ShieldCheck className="h-4 w-4 text-indigo-500" />
            <span>Workspace & Team</span>
          </CommandItem>
        </CommandGroup>

        <CommandSeparator />

        <CommandGroup heading="Theme">
          <CommandItem
            onSelect={() => handleSelect(() => setTheme('light'))}
            className="flex items-center gap-2 cursor-pointer"
          >
            <Sun className="h-4 w-4 text-amber-500" />
            <span>Light Theme</span>
          </CommandItem>
          <CommandItem
            onSelect={() => handleSelect(() => setTheme('dark'))}
            className="flex items-center gap-2 cursor-pointer"
          >
            <Moon className="h-4 w-4 text-blue-400" />
            <span>Dark Theme</span>
          </CommandItem>
          <CommandItem
            onSelect={() => handleSelect(() => setTheme('system'))}
            className="flex items-center gap-2 cursor-pointer"
          >
            <Laptop className="h-4 w-4 text-muted-foreground" />
            <span>System Default</span>
          </CommandItem>
        </CommandGroup>

        <CommandSeparator />

        <CommandGroup heading="Quick Actions">
          <CommandItem
            onSelect={() => handleSelect(copyWorkspaceId)}
            className="flex items-center gap-2 cursor-pointer"
          >
            <Copy className="h-4 w-4 text-muted-foreground" />
            <span>{copiedWs ? 'Workspace ID Copied!' : 'Copy Active Workspace ID'}</span>
          </CommandItem>
          <CommandItem
            onSelect={() => handleSelect(signOut)}
            className="flex items-center gap-2 text-destructive cursor-pointer"
          >
            <LogOut className="h-4 w-4 text-destructive" />
            <span>Sign Out</span>
          </CommandItem>
        </CommandGroup>
      </CommandList>
    </CommandDialog>
  )
}
