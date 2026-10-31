import { useState } from "react";
import { Check, Copy } from "lucide-react";
import { content } from "@/content";
import { Reveal, SectionTitle } from "./primitives";
import { cn } from "@/lib/utils";

export function Integration() {
  const tabs = content.integration.tabs;
  const [active, setActive] = useState<string>(tabs[0]!.id);
  const [copied, setCopied] = useState(false);
  const tab = tabs.find((t) => t.id === active)!;

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(tab.code);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {}
  };

  return (
    <section className="section" aria-labelledby="int-title">
      <div className="container-page">
        <SectionTitle id="int-title" eyebrow="Integration" parts={content.integration.title} />
        <Reveal className="mx-auto max-w-3xl">
          <div className="overflow-hidden rounded-3xl border border-border bg-card">
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border px-4 py-3">
              <div role="tablist" aria-label="Integration method" className="flex gap-1 rounded-full bg-secondary p-1">
                {tabs.map((t) => (
                  <button
                    key={t.id}
                    role="tab"
                    aria-selected={active === t.id}
                    onClick={() => setActive(t.id)}
                    className={cn(
                      "rounded-full px-4 py-1.5 text-sm transition-colors",
                      active === t.id ? "bg-background text-foreground shadow-sm" : "text-muted-foreground hover:text-foreground",
                    )}
                  >
                    {t.label}
                  </button>
                ))}
              </div>
              <button onClick={copy} className="inline-flex items-center gap-2 rounded-full border border-border px-3 py-1.5 text-xs text-muted-foreground hover:text-foreground">
                {copied ? <Check className="h-3.5 w-3.5 text-success" /> : <Copy className="h-3.5 w-3.5" />}
                {copied ? "Copied" : "Copy"}
              </button>
            </div>
            <pre role="tabpanel" className="overflow-x-auto p-6 font-mono text-sm leading-7 text-foreground/90">
              <code>{tab.code}</code>
            </pre>
          </div>
        </Reveal>
      </div>
    </section>
  );
}
