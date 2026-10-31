import { Link } from "@tanstack/react-router";
import { pill } from "./primitives";

export function Placeholder({ title, body }: { title: string; body: string }) {
  return (
    <main className="flex min-h-screen items-center justify-center px-6 text-center">
      <div className="max-w-md">
        <h1 className="text-4xl font-bold tracking-tight">{title}</h1>
        <p className="mt-4 text-muted-foreground">{body}</p>
        <Link to="/" className={pill({ variant: "outline", className: "mt-8" })}>Back home</Link>
      </div>
    </main>
  );
}
