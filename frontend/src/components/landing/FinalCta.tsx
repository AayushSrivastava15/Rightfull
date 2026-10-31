import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight, CheckCircle2 } from "lucide-react";
import { Link } from "@tanstack/react-router";
import { content } from "@/content";
import { waitlistSchema, type Waitlist } from "@/lib/schemas";
import { Reveal, pill } from "./primitives";

export function FinalCta() {
  const [done, setDone] = useState(false);
  const { register, handleSubmit, formState: { errors } } = useForm<Waitlist>({ resolver: zodResolver(waitlistSchema) });

  return (
    <section className="section relative overflow-hidden" aria-labelledby="cta-title">
      <div aria-hidden className="absolute left-1/2 top-1/2 h-80 w-[44rem] -translate-x-1/2 -translate-y-1/2 rounded-full bg-brand/15 blur-[120px]" />
      <Reveal className="container-page relative text-center">
        <h2 id="cta-title" className="mx-auto max-w-4xl text-5xl font-bold tracking-tighter md:text-7xl">
          {content.cta.title[0]}
          <span className="text-gradient">{content.cta.title[1]}</span>
        </h2>
        <div className="mt-12">
          <Link to="/signup" className={pill()}>
            {content.cta.button} <ArrowRight className="h-4 w-4" />
          </Link>
        </div>
        <p className="mt-14 text-sm text-muted-foreground">{content.cta.waitlist}</p>
        {done ? (
          <p role="status" className="mt-4 inline-flex items-center gap-2 text-success">
            <CheckCircle2 className="h-4 w-4" /> You're on the list. We'll be in touch.
          </p>
        ) : (
          <form onSubmit={handleSubmit(() => setDone(true))} noValidate className="mx-auto mt-4 max-w-md">
            <div className="flex gap-2 rounded-full border border-border bg-card p-1.5">
              <label htmlFor="wl-email" className="sr-only">Email</label>
              <input
                id="wl-email"
                type="email"
                placeholder="you@company.com"
                aria-invalid={!!errors.email}
                aria-describedby={errors.email ? "wl-err" : undefined}
                {...register("email")}
                className="min-w-0 flex-1 bg-transparent px-4 text-sm outline-none placeholder:text-muted-foreground"
              />
              <button type="submit" className={pill({ size: "md" })}>Join</button>
            </div>
            {errors.email && <p id="wl-err" className="mt-3 text-sm text-destructive">{errors.email.message}</p>}
          </form>
        )}
      </Reveal>
    </section>
  );
}
