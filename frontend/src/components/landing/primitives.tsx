import { cva, type VariantProps } from "class-variance-authority";
import { motion, useReducedMotion } from "motion/react";
import { useRef, type ReactNode, type MouseEvent } from "react";
import { cn } from "@/lib/utils";

export const pill = cva(
  "inline-flex items-center justify-center gap-2 rounded-full text-sm font-medium transition-all duration-200 hover:-translate-y-0.5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-50 select-none cursor-pointer",
  {
    variants: {
      variant: {
        hero: "bg-primary text-primary-foreground glow-cta hover:opacity-95",
        primary: "bg-blue-600 hover:bg-blue-500 text-white shadow-sm shadow-blue-500/20 active:translate-y-0",
        outline: "border border-border bg-background/40 text-foreground backdrop-blur hover:bg-secondary",
        ghost: "text-muted-foreground hover:text-foreground hover:bg-secondary/70",
      },
      size: {
        lg: "h-12 px-7 text-sm sm:text-base",
        md: "h-10 px-5 text-sm",
        sm: "h-9 px-4 text-xs font-semibold",
        icon: "h-9 w-9 p-0",
      },
    },
    defaultVariants: { variant: "hero", size: "lg" },
  },
);
export type PillProps = VariantProps<typeof pill>;

export function Reveal({ children, delay = 0, className }: { children: ReactNode; delay?: number; className?: string | undefined }) {
  const reduce = useReducedMotion();
  return (
    <motion.div
      className={className}
      initial={reduce ? false : { opacity: 0, y: 20 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: "-60px" }}
      transition={{ duration: 0.5, delay, ease: "easeOut" }}
    >
      {children}
    </motion.div>
  );
}

export function SectionTitle({ parts, eyebrow, id }: { parts: string[]; eyebrow: string; id?: string }) {
  return (
    <Reveal className="mx-auto mb-16 max-w-2xl text-center md:mb-20">
      <span className="inline-block rounded-full border border-border px-4 py-1 text-xs uppercase tracking-widest text-muted-foreground">
        {eyebrow}
      </span>
      <h2 id={id} className="mt-6 text-4xl font-bold tracking-tight md:text-5xl">
        {parts[0]}
        <span className="text-gradient">{parts[1]}</span>
      </h2>
    </Reveal>
  );
}

/** Card with cursor-following border glow (desktop pointer only). */
export function GlowCard({ children, className, highlight }: { children: ReactNode; className?: string | undefined; highlight?: boolean | undefined }) {
  const ref = useRef<HTMLDivElement>(null);
  const onMove = (e: MouseEvent) => {
    const el = ref.current;
    if (!el) return;
    const r = el.getBoundingClientRect();
    el.style.setProperty("--x", `${e.clientX - r.left}px`);
    el.style.setProperty("--y", `${e.clientY - r.top}px`);
  };
  return (
    <div
      ref={ref}
      onMouseMove={onMove}
      className={cn(
        "group relative overflow-hidden rounded-3xl border bg-card/60 p-8 transition-colors",
        highlight ? "border-brand/50 glow-brand" : "border-border",
        className,
      )}
    >
      <div
        aria-hidden
        className="pointer-events-none absolute inset-0 opacity-0 transition-opacity duration-300 group-hover:opacity-100 [@media(hover:none)]:hidden"
        style={{
          background:
            "radial-gradient(400px circle at var(--x) var(--y), color-mix(in oklab, var(--brand) 14%, transparent), transparent 60%)",
        }}
      />
      <div className="relative">{children}</div>
    </div>
  );
}
