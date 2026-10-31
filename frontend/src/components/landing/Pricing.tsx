import { Check } from "lucide-react";
import { Link } from "@tanstack/react-router";
import { content } from "@/content";
import { GlowCard, Reveal, SectionTitle, pill } from "./primitives";

export function Pricing() {
  return (
    <section id="pricing" className="section" aria-labelledby="pricing-title">
      <div className="container-page">
        <SectionTitle id="pricing-title" eyebrow="Pricing" parts={content.pricing.title} />
        <div className="mx-auto grid max-w-4xl gap-8 md:grid-cols-2">
          {content.pricing.plans.map((p, i) => (
            <Reveal key={p.name} delay={i * 0.1}>
              <GlowCard highlight={p.highlight} className="flex h-full flex-col p-10">
                <h3 className="text-lg font-medium text-muted-foreground">{p.name}</h3>
                <p className="mt-4 text-5xl font-bold tracking-tight">{p.price}</p>
                <ul className="mt-10 space-y-4">
                  {p.features.map((f) => (
                    <li key={f} className="flex items-center gap-3 text-muted-foreground">
                      <Check className="h-4 w-4 shrink-0 text-brand-glow" aria-hidden /> {f}
                    </li>
                  ))}
                </ul>
                <Link to={p.href as "/signup" | "/contact"} className={pill({ variant: p.highlight ? "hero" : "outline", className: "mt-12 w-full" })}>
                  {p.cta}
                </Link>
              </GlowCard>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}
