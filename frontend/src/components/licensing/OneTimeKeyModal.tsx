import React, { useState } from 'react'
import { Check, Copy, KeyRound, ShieldAlert, Sparkles } from 'lucide-react'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'

interface OneTimeKeyModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  licenseKey: string
  productName: string
  isPerpetual?: boolean
  expiresAt?: string
}

export function OneTimeKeyModal({
  open,
  onOpenChange,
  licenseKey,
  productName,
  isPerpetual,
  expiresAt,
}: OneTimeKeyModalProps) {
  const [copied, setCopied] = useState(false)

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(licenseKey)
      setCopied(true)
      setTimeout(() => setCopied(false), 2500)
    } catch (e) {
      console.warn('Clipboard write failed:', e)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-xl border-border bg-card shadow-2xl">
        <DialogHeader>
          <div className="flex items-center gap-2 mb-1">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <KeyRound className="h-5 w-5" />
            </div>
            <div>
              <DialogTitle className="text-lg font-bold text-foreground">
                License Key Issued Successfully
              </DialogTitle>
              <DialogDescription className="text-xs text-muted-foreground">
                Cryptographically generated for {productName}
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="space-y-4 py-2">
          {/* Security Alert Banner */}
          <div className="rounded-xl border border-amber-500/30 bg-amber-500/10 p-3.5 flex items-start gap-3">
            <ShieldAlert className="h-5 w-5 text-amber-500 shrink-0 mt-0.5" />
            <div className="text-xs text-amber-700 dark:text-amber-300 space-y-1">
              <p className="font-semibold">Save this key now. It will never be shown again.</p>
              <p className="text-[11px] opacity-90">
                For cryptographic security, rightfull persists only an HMAC-SHA-256 digest in PostgreSQL.
                Once closed, this plaintext key cannot be recovered from the dashboard or API.
              </p>
            </div>
          </div>

          {/* Key Display Container */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span>Plaintext License Key</span>
              <div className="flex items-center gap-2">
                {isPerpetual ? (
                  <Badge variant="outline" className="text-[10px] bg-emerald-500/10 text-emerald-500 border-emerald-500/30">
                    Perpetual License
                  </Badge>
                ) : expiresAt ? (
                  <Badge variant="outline" className="text-[10px]">
                    Expires {new Date(expiresAt).toLocaleDateString()}
                  </Badge>
                ) : null}
              </div>
            </div>

            <div className="flex items-center justify-between rounded-xl border border-border bg-muted/40 p-3.5 font-mono text-sm tracking-wide text-foreground shadow-inner select-all">
              <span className="font-semibold text-primary">{licenseKey}</span>
              <Button
                variant="outline"
                size="sm"
                onClick={handleCopy}
                className="gap-1.5 h-8 text-xs font-sans shrink-0 ml-3"
              >
                {copied ? (
                  <>
                    <Check className="h-3.5 w-3.5 text-emerald-500" />
                    <span className="text-emerald-500 font-medium">Copied!</span>
                  </>
                ) : (
                  <>
                    <Copy className="h-3.5 w-3.5" />
                    <span>Copy Key</span>
                  </>
                )}
              </Button>
            </div>
          </div>
        </div>

        <DialogFooter className="mt-2">
          <Button
            type="button"
            onClick={() => onOpenChange(false)}
            className="w-full text-xs font-medium"
          >
            I Have Securely Saved This Key
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

