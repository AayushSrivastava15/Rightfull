import React, { useState } from 'react'
import {
  Code2,
  Terminal,
  KeyRound,
  ShieldCheck,
  ExternalLink,
  Cpu,
  Layers,
  Sparkles,
  BookOpen,
} from 'lucide-react'
import { TerminalBlock } from '@/components/ui/terminal-block'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'

interface DeveloperDocsViewProps {
  workspaceId: string
}

export function DeveloperDocsView({ workspaceId }: DeveloperDocsViewProps) {
  const [activeLang, setActiveLang] = useState<'curl' | 'typescript' | 'python'>('curl')

  const curlListProducts = `curl -X GET "http://localhost:8080/api/v1/products?workspaceId=${workspaceId}" \\
  -H "Authorization: Bearer <SUPABASE_JWT_TOKEN>" \\
  -H "Content-Type: application/json"`

  const curlCreatePlan = `curl -X POST "http://localhost:8080/api/v1/products/<PRODUCT_ID>/license-configurations?workspaceId=${workspaceId}" \\
  -H "Authorization: Bearer <SUPABASE_JWT_TOKEN>" \\
  -H "Content-Type: application/json" \\
  -d '{
    "name": "Developer Pro",
    "description": "Standard developer plan with API access",
    "durationDays": 365,
    "maximumActivations": 2,
    "seatLimit": 1,
    "offlineGracePeriodSeconds": 25200,
    "revalidationIntervalSeconds": 300,
    "entitlements": {
      "export_csv": true,
      "max_projects": 50,
      "tier": "pro"
    }
  }'`

  const tsSdkExample = `import { RightfulClient } from '@rightfull/sdk';

// Initialize client with workspace credentials
const client = new RightfulClient({
  workspaceId: '${workspaceId}',
  apiKey: process.env.RIGHTFUL_API_KEY
});

// Fetch active product catalog and cryptographic signing keys
const products = await client.products.list();
console.log('Registered products:', products);

// Retrieve active license configuration presets
const plans = await client.configurations.list({ productId: products[0].id });
console.log('Available plans:', plans);`

  const ed25519VerificationCode = `import crypto from 'node:crypto';

// Verify offline license token signed by Rightful's Ed25519 private key
export function verifyLicenseSignature(
  payloadBytes: Uint8Array,
  signatureBase64: string,
  publicKeyBase64: string
): boolean {
  const publicKey = crypto.createPublicKey({
    key: Buffer.from(publicKeyBase64, 'base64'),
    format: 'der',
    type: 'spki'
  });

  return crypto.verify(
    null, // Ed25519 does not hash before signing
    payloadBytes,
    publicKey,
    Buffer.from(signatureBase64, 'base64')
  );
}`

  return (
    <div className="space-y-8 animate-in fade-in-50 duration-300">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-foreground">
            Developer Experience & API Documentation
          </h2>
          <p className="text-xs text-muted-foreground mt-0.5">
            Integrate Rightful software licensing into your desktop, mobile, CLI, or cloud applications.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => window.open('http://localhost:8080/swagger-ui.html', '_blank')}
            className="text-xs gap-1.5"
          >
            <span>Swagger UI</span>
            <ExternalLink className="h-3.5 w-3.5" />
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => window.open('http://localhost:8080/v3/api-docs', '_blank')}
            className="text-xs gap-1.5"
          >
            <span>OpenAPI 3.1 JSON</span>
            <ExternalLink className="h-3.5 w-3.5" />
          </Button>
        </div>
      </div>

      {/* Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="rounded-xl border border-border/70 bg-card p-5 space-y-2 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-foreground flex items-center gap-2">
              <ShieldCheck className="h-4 w-4 text-primary" />
              Authentication
            </span>
            <Badge variant="secondary" className="text-[10px] font-mono">
              BEARER JWT
            </Badge>
          </div>
          <p className="text-xs text-muted-foreground leading-relaxed">
            All API requests require a valid Supabase JWT token passed in the <code className="text-foreground font-mono bg-muted px-1 py-0.5 rounded">Authorization: Bearer</code> header.
          </p>
        </div>

        <div className="rounded-xl border border-border/70 bg-card p-5 space-y-2 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-foreground flex items-center gap-2">
              <KeyRound className="h-4 w-4 text-emerald-500" />
              Cryptography Standard
            </span>
            <Badge variant="secondary" className="text-[10px] font-mono text-emerald-500">
              Ed25519
            </Badge>
          </div>
          <p className="text-xs text-muted-foreground leading-relaxed">
            Licenses use modern Edwards-curve Digital Signature Algorithm (Ed25519) with 256-bit security, verified instantly on client devices without phone-home requirements.
          </p>
        </div>

        <div className="rounded-xl border border-border/70 bg-card p-5 space-y-2 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-foreground flex items-center gap-2">
              <Cpu className="h-4 w-4 text-amber-500" />
              SDK Status
            </span>
            <Badge variant="outline" className="text-[10px] font-mono text-muted-foreground">
              PREVIEW
            </Badge>
          </div>
          <p className="text-xs text-muted-foreground leading-relaxed">
            Native client SDKs for Node.js, Electron, Python, Go, and Rust are in active preview. Direct REST API endpoints are fully production operational.
          </p>
        </div>
      </div>

      {/* Interactive Code Examples */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-foreground">API Code Examples</h3>
          <div className="flex items-center gap-1 border border-border/70 rounded-lg p-0.5 bg-card">
            {(['curl', 'typescript', 'python'] as const).map((lang) => (
              <button
                key={lang}
                onClick={() => setActiveLang(lang)}
                className={`rounded-md px-3 py-1 text-xs font-medium transition-colors uppercase ${
                  activeLang === lang
                    ? 'bg-primary text-primary-foreground font-semibold shadow-xs'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                {lang}
              </button>
            ))}
          </div>
        </div>

        {activeLang === 'curl' && (
          <div className="space-y-4">
            <div>
              <p className="text-xs font-medium text-foreground mb-1.5">
                1. List Products in Workspace
              </p>
              <TerminalBlock
                title="curl - List Products"
                code={curlListProducts}
                language="bash"
              />
            </div>

            <div>
              <p className="text-xs font-medium text-foreground mb-1.5">
                2. Create License Plan with JSON Entitlements
              </p>
              <TerminalBlock
                title="curl - Create License Configuration"
                code={curlCreatePlan}
                language="bash"
              />
            </div>
          </div>
        )}

        {activeLang === 'typescript' && (
          <div className="space-y-4">
            <div>
              <p className="text-xs font-medium text-foreground mb-1.5">
                TypeScript SDK Initialization & Listing
              </p>
              <TerminalBlock
                title="client.ts - Rightful SDK"
                code={tsSdkExample}
                language="typescript"
              />
            </div>

            <div>
              <p className="text-xs font-medium text-foreground mb-1.5">
                Client-Side Ed25519 License Signature Verification
              </p>
              <TerminalBlock
                title="verify-signature.ts - Offline Cryptography"
                code={ed25519VerificationCode}
                language="typescript"
              />
            </div>
          </div>
        )}

        {activeLang === 'python' && (
          <div>
            <p className="text-xs font-medium text-foreground mb-1.5">
              Python REST Integration (requests)
            </p>
            <TerminalBlock
              title="rightful_client.py"
              code={`import requests

WORKSPACE_ID = "${workspaceId}"
API_URL = "http://localhost:8080/api/v1"
HEADERS = {
    "Authorization": "Bearer <SUPABASE_JWT_TOKEN>",
    "Content-Type": "application/json"
}

# Fetch products
resp = requests.get(f"{API_URL}/products?workspaceId={WORKSPACE_ID}", headers=HEADERS)
products = resp.json()["data"]
print("Products:", [p["name"] for p in products])`}
              language="python"
            />
          </div>
        )}
      </div>
    </div>
  )
}

