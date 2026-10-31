import { KeyRound } from "lucide-react";
import { content } from "@/content";

export function Footer() {
  return (
    <footer className="border-t border-border py-12">
      <div className="container-page flex flex-col items-center justify-between gap-6 text-sm text-muted-foreground md:flex-row">
        <div className="flex items-center gap-2 font-semibold text-foreground">
          <KeyRound className="h-4 w-4 text-brand-glow" aria-hidden /> License Hub
          <span className="ml-2 rounded-full border border-border px-3 py-0.5 text-xs font-normal text-muted-foreground">Open source</span>
        </div>
        <ul className="flex gap-6">
          {content.footer.map((l) => (
            <li key={l.label}><a href={l.href} className="hover:text-foreground">{l.label}</a></li>
          ))}
        </ul>
        <p>© {new Date().getFullYear()} License Hub</p>
      </div>
    </footer>
  );
}
