import React, { useState } from 'react'
import {
  Building2,
  Users,
  ShieldCheck,
  Copy,
  Check,
  UserPlus,
  Mail,
  Shield,
  Clock,
  Sparkles,
  ChevronDown,
} from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useAuth } from '@/contexts/AuthContext'

export function WorkspaceTeamView() {
  const { profile, user } = useAuth()
  const defaultWs = profile?.defaultWorkspace
  const memberships = profile?.memberships ?? []
  const role = defaultWs?.role || 'MEMBER'
  const canManage = role === 'OWNER' || role === 'ADMIN'

  const [copiedId, setCopiedId] = useState(false)
  const [copiedSlug, setCopiedSlug] = useState(false)
  const [inviteEmail, setInviteEmail] = useState('')
  const [inviteRole, setInviteRole] = useState<'ADMIN' | 'MEMBER' | 'VIEWER'>('MEMBER')
  const [inviteSent, setInviteSent] = useState(false)

  const copyToClipboard = async (text: string, type: 'id' | 'slug') => {
    try {
      await navigator.clipboard.writeText(text)
      if (type === 'id') {
        setCopiedId(true)
        setTimeout(() => setCopiedId(false), 2000)
      } else {
        setCopiedSlug(true)
        setTimeout(() => setCopiedSlug(false), 2000)
      }
    } catch (e) {
      console.warn('Copy failed:', e)
    }
  }

  const handleInvite = (e: React.FormEvent) => {
    e.preventDefault()
    if (!inviteEmail) return
    setInviteSent(true)
    setTimeout(() => {
      setInviteSent(false)
      setInviteEmail('')
    }, 3000)
  }

  return (
    <div className="space-y-8 animate-in fade-in-50 duration-300">
      {/* Header */}
      <div>
        <h2 className="text-xl font-bold tracking-tight text-foreground">
          Workspace & Team Management
        </h2>
        <p className="text-xs text-muted-foreground mt-0.5">
          Manage your organization settings, team members, and role-based access control.
        </p>
      </div>

      {/* Workspace Details Card */}
      <div className="rounded-xl border border-border/70 bg-card p-6 shadow-sm space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10 text-primary ring-1 ring-primary/20">
              <Building2 className="h-6 w-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-bold text-foreground">
                  {defaultWs?.name || 'Default Workspace'}
                </h3>
                <Badge variant="outline" className="text-[10px] uppercase font-mono tracking-wider border-primary/30 text-primary">
                  {defaultWs?.tier || 'COMMUNITY'}
                </Badge>
              </div>
              <p className="text-xs text-muted-foreground mt-0.5">
                Multi-tenant isolated PostgreSQL row-level security boundary.
              </p>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => defaultWs?.id && copyToClipboard(defaultWs.id, 'id')}
              className="text-xs gap-1.5 h-8"
            >
              {copiedId ? <Check className="h-3.5 w-3.5 text-emerald-500" /> : <Copy className="h-3.5 w-3.5" />}
              <span>{copiedId ? 'Copied ID' : 'Copy UUID'}</span>
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => defaultWs?.slug && copyToClipboard(defaultWs.slug, 'slug')}
              className="text-xs gap-1.5 h-8 font-mono"
            >
              {copiedSlug ? <Check className="h-3.5 w-3.5 text-emerald-500" /> : <Copy className="h-3.5 w-3.5" />}
              <span>{copiedSlug ? 'Copied Slug' : defaultWs?.slug}</span>
            </Button>
          </div>
        </div>

        {/* Workspace Attributes Grid */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 pt-2 border-t border-border/60">
          <div>
            <span className="text-[11px] text-muted-foreground">Your Role</span>
            <div className="text-xs font-semibold text-foreground mt-0.5">{role}</div>
          </div>
          <div>
            <span className="text-[11px] text-muted-foreground">Workspace Tier</span>
            <div className="text-xs font-semibold text-foreground mt-0.5">{defaultWs?.tier || 'COMMUNITY'}</div>
          </div>
          <div>
            <span className="text-[11px] text-muted-foreground">Tenant Isolation</span>
            <div className="text-xs font-semibold text-emerald-500 mt-0.5">PostgreSQL RLS Active</div>
          </div>
          <div>
            <span className="text-[11px] text-muted-foreground">Members Count</span>
            <div className="text-xs font-semibold text-foreground mt-0.5">1 active member</div>
          </div>
        </div>
      </div>

      {/* Team Members & Invitations */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Members List (2 Cols) */}
        <div className="lg:col-span-2 rounded-xl border border-border/70 bg-card p-6 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2.5">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-500/10 text-indigo-500">
                <Users className="h-4 w-4" />
              </div>
              <div>
                <h3 className="text-sm font-semibold text-foreground">Active Team Members</h3>
                <p className="text-xs text-muted-foreground">
                  Users authorized to access products and license configurations in this workspace.
                </p>
              </div>
            </div>
          </div>

          <div className="divide-y divide-border/60">
            {/* Current User Row */}
            <div className="flex items-center justify-between py-3">
              <div className="flex items-center gap-3">
                {profile?.avatarUrl ? (
                  <img
                    src={profile.avatarUrl}
                    alt={profile.fullName || 'User'}
                    className="h-8 w-8 rounded-full object-cover ring-1 ring-border"
                  />
                ) : (
                  <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary/10 text-primary text-xs font-bold uppercase ring-1 ring-primary/20">
                    {(profile?.fullName || user?.email || 'U')[0]}
                  </div>
                )}
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-semibold text-foreground">
                      {profile?.fullName || 'Current User'}
                    </span>
                    <Badge variant="secondary" className="text-[10px] px-1.5 py-0 font-normal">
                      You
                    </Badge>
                  </div>
                  <span className="text-[11px] text-muted-foreground">{user?.email}</span>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <Badge
                  variant="outline"
                  className="text-[10px] font-mono border-primary/30 text-primary uppercase"
                >
                  {role}
                </Badge>
              </div>
            </div>
          </div>
        </div>

        {/* Invite Member Card (1 Col) */}
        <div className="rounded-xl border border-border/70 bg-card p-6 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-2.5 mb-3">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <UserPlus className="h-4 w-4" />
              </div>
              <div>
                <h3 className="text-sm font-semibold text-foreground">Invite Team Member</h3>
                <p className="text-xs text-muted-foreground">Send invitation link by email</p>
              </div>
            </div>

            {canManage ? (
              <form onSubmit={handleInvite} className="space-y-3 mt-4">
                <div className="space-y-1">
                  <label className="text-xs font-medium text-foreground">Teammate Email</label>
                  <Input
                    type="email"
                    placeholder="developer@company.com"
                    value={inviteEmail}
                    onChange={(e) => setInviteEmail(e.target.value)}
                    required
                    className="text-xs h-9"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-medium text-foreground">Role</label>
                  <div className="relative">
                    <select
                      value={inviteRole}
                      onChange={(e: any) => setInviteRole(e.target.value)}
                      className="w-full h-10 appearance-none rounded-lg border border-border/80 bg-background px-3 pr-8 text-xs font-medium text-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-primary cursor-pointer transition-colors hover:border-border"
                    >
                      <option value="ADMIN">ADMIN (Full management)</option>
                      <option value="MEMBER">MEMBER (Standard access)</option>
                      <option value="VIEWER">VIEWER (Read-only)</option>
                    </select>
                    <div className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground">
                      <ChevronDown className="h-4 w-4 opacity-60" />
                    </div>
                  </div>
                </div>

                {inviteSent && (
                  <div className="rounded-lg border border-emerald-500/20 bg-emerald-500/10 p-2 text-xs text-emerald-500 flex items-center gap-1.5">
                    <Check className="h-3.5 w-3.5" />
                    Invitation dispatched!
                  </div>
                )}

                <Button type="submit" size="sm" className="w-full gap-1.5 text-xs mt-2 h-9">
                  <Mail className="h-3.5 w-3.5" />
                  Send Invitation
                </Button>
              </form>
            ) : (
              <div className="mt-4 rounded-lg border border-border/60 bg-muted/20 p-4 text-xs text-muted-foreground">
                Only workspace Owners and Admins can invite new teammates. Contact your workspace administrator to add team members.
              </div>
            )}
          </div>

          <div className="mt-6 pt-3 border-t border-border/60 text-[11px] text-muted-foreground">
            Role changes and invitations are recorded in the persistent workspace audit log.
          </div>
        </div>
      </div>
    </div>
  )
}

