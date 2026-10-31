import { motion, useReducedMotion } from "motion/react";
import { ArrowRight, Sparkles } from "lucide-react";
import { Link } from "@tanstack/react-router";
import { content } from "@/content";
import { pill } from "./primitives";

function WaveLines() {
  const lines = Array.from({ length: 18 }, (_, i) => i);
  return (
    <div
      aria-hidden
      className="pointer-events-none absolute inset-0 overflow-hidden"
      style={{ maskImage: "radial-gradient(ellipse 70% 60% at 50% 45%, black 20%, transparent 75%)", WebkitMaskImage: "radial-gradient(ellipse 70% 60% at 50% 45%, black 20%, transparent 75%)" }}
    >
      <svg className="animate-wave absolute -left-[10%] top-0 h-full w-[120%] opacity-60" viewBox="0 0 1200 800" preserveAspectRatio="none">
        <defs>
          <linearGradient id="wl" x1="0" x2="1">
            <stop offset="0" stopColor="var(--brand-glow)" stopOpacity="0" />
            <stop offset="0.5" stopColor="var(--brand)" stopOpacity="0.9" />
            <stop offset="1" stopColor="var(--brand-glow)" stopOpacity="0" />
          </linearGradient>
        </defs>
        {lines.map((i) => (
          <path
            key={i}
            d={`M -50 ${700 - i * 22} C 300 ${520 - i * 30}, 650 ${760 - i * 18}, 1250 ${120 + i * 14}`}
            fill="none"
            stroke="url(#wl)"
            strokeWidth={i % 4 === 0 ? 1.4 : 0.7}
            opacity={0.25 + (i % 5) * 0.12}
          />
        ))}
      </svg>
      <div className="absolute left-1/2 top-1/3 h-72 w-[40rem] -translate-x-1/2 rounded-full bg-brand/20 blur-[120px]" />
    </div>
  );
}

export function Hero() {
  const reduce = useReducedMotion();
  const h = content.hero;
  const fade = (d: number) => ({
    initial: reduce ? false : { opacity: 0, y: 20 },
    animate: { opacity: 1, y: 0 },
    transition: { duration: 0.6, delay: d, ease: "easeOut" as const },
  });
  return (
    <section id="top" className="relative flex min-h-[100svh] items-center overflow-hidden pb-24 pt-40">
      <WaveLines />
      <div className="container-page relative text-center">
        <motion.span {...fade(0)} className="inline-flex items-center gap-2 rounded-full border border-border bg-background/50 px-4 py-1.5 text-sm text-muted-foreground backdrop-blur">
          <Sparkles className="h-3.5 w-3.5 text-brand-glow" aria-hidden /> {h.badge}
        </motion.span>
        <motion.h1 {...fade(0.1)} className="mx-auto mt-8 max-w-5xl text-5xl font-bold leading-[1.02] tracking-tighter sm:text-6xl md:text-7xl lg:text-8xl">
          {h.line1}
          <br />
          <span className="text-gradient depth-text">{h.line2}</span>
        </motion.h1>
        <motion.p {...fade(0.2)} className="mx-auto mt-8 max-w-2xl text-lg leading-relaxed text-muted-foreground">
          {h.sub}
        </motion.p>
        <motion.div {...fade(0.3)} className="mt-10 flex flex-col items-center justify-center gap-4 sm:flex-row">
          <Link to="/signup" className={pill()}>
            {h.primary} <ArrowRight className="h-4 w-4" />
          </Link>
          <Link to="/docs" className={pill({ variant: "outline" })}>{h.secondary}</Link>
        </motion.div>
        <motion.p {...fade(0.4)} className="mt-6 text-sm text-muted-foreground/80">{h.note}</motion.p>
      </div>
    </section>
  );
}
