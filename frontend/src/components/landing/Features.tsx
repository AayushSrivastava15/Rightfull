import { Activity, Ban, Code2, Github, ShieldCheck, Users } from "lucide-react";
import { content } from "@/content";
import { GlowCard, Reveal, SectionTitle } from "./primitives";

const icons = { revoke: Ban, seats: Users, tokens: ShieldCheck, log: Activity, sdk: Code2, oss: Github };
const spans: Record<string, string> = {
  revoke: "md:col-span-2",
  seats: "",
  tokens: "",
  log: "md:col-span-2",
  sdk: "md:col-span-2",
  oss: "",
};

export function Features() {
  return (
    <section id="features" className="section" aria-labelledby="features-title">
      <div className="container-page">
        <SectionTitle id="features-title" eyebrow="Features" parts={content.features.title} />
        <div className="grid gap-6 md:grid-cols-3">
          {content.features.items.map((f, i) => {
            const Icon = icons[f.key as keyof typeof icons];
            return (
              <Reveal key={f.key} delay={(i % 3) * 0.08} className={spans[f.key]}>
                <GlowCard className="flex h-full min-h-56 flex-col">
                  <span className="grid h-11 w-11 place-items-center rounded-full border border-border bg-accent text-accent-foreground">
                    <Icon className="h-5 w-5" aria-hidden />
                  </span>
                  <h3 className="mt-8 text-xl font-semibold">{f.title}</h3>
                  <p className="mt-3 max-w-md leading-relaxed text-muted-foreground">{f.body}</p>
                </GlowCard>
              </Reveal>
            );
          })}
        </div>
      </div>
    </section>
  );
}
