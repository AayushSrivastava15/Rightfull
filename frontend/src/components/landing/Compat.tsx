import { content } from "@/content";

export function Compat() {
  const items = [...content.compat, ...content.compat, ...content.compat, ...content.compat];
  return (
    <section aria-label="Works with" className="border-y border-border py-10">
      <div className="relative overflow-hidden" style={{ maskImage: "linear-gradient(90deg, transparent, black 15%, black 85%, transparent)", WebkitMaskImage: "linear-gradient(90deg, transparent, black 15%, black 85%, transparent)" }}>
        <ul className="animate-marquee flex w-max gap-6">
          {items.map((t, i) => (
            <li key={i} aria-hidden={i >= content.compat.length} className="rounded-full border border-border px-6 py-2 text-sm text-muted-foreground">
              {t}
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
