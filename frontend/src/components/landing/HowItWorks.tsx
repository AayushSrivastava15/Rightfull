import { content } from "@/content";
import { GlowCard, Reveal, SectionTitle } from "./primitives";

export function HowItWorks() {
  return (
    <section id="how" className="section" aria-labelledby="how-title">
      <div className="container-page">
        <SectionTitle id="how-title" eyebrow="How it works" parts={content.how.title} />
        <div className="relative grid gap-8 md:grid-cols-3">
          <svg aria-hidden className="pointer-events-none absolute left-0 top-16 hidden h-4 w-full md:block" viewBox="0 0 1000 10" preserveAspectRatio="none">
            <line x1="120" y1="5" x2="880" y2="5" stroke="var(--border)" strokeWidth="1" />
            <line className="animate-beam" x1="120" y1="5" x2="880" y2="5" stroke="var(--brand-glow)" strokeWidth="2" strokeLinecap="round" />
          </svg>
          {content.how.steps.map((s, i) => (
            <Reveal key={s.title} delay={i * 0.1}>
              <GlowCard className="h-full bg-card">
                <span className="text-gradient text-6xl font-bold tracking-tighter">0{i + 1}</span>
                <h3 className="mt-6 text-xl font-semibold leading-snug">{s.title}</h3>
                <p className="mt-3 leading-relaxed text-muted-foreground">{s.body}</p>
              </GlowCard>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}
