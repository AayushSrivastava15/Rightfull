import { content } from "@/content";
import { Reveal, SectionTitle } from "./primitives";
import { cn } from "@/lib/utils";

const statusClass: Record<string, string> = {
  Active: "bg-success/15 text-success border-success/30",
  Revoked: "bg-destructive/15 text-destructive border-destructive/30",
  Expired: "bg-warning/15 text-warning border-warning/30",
};

export function DashboardPreview() {
  return (
    <section className="section overflow-hidden" aria-labelledby="dash-title">
      <div className="container-page">
        <SectionTitle id="dash-title" eyebrow="Dashboard" parts={content.dashboard.title} />
        <Reveal>
          <div className="mx-auto max-w-5xl [perspective:1600px]">
            <div className="rounded-3xl border border-brand/40 bg-card p-2 glow-brand md:[transform:rotateX(8deg)]">
              <div className="flex items-center gap-2 border-b border-border px-4 py-3">
                <span className="h-2.5 w-2.5 rounded-full bg-muted-foreground/30" />
                <span className="h-2.5 w-2.5 rounded-full bg-muted-foreground/30" />
                <span className="h-2.5 w-2.5 rounded-full bg-muted-foreground/30" />
                <span className="ml-3 text-sm text-muted-foreground">Activation log</span>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[640px] text-left text-sm">
                  <thead className="text-xs uppercase tracking-wider text-muted-foreground">
                    <tr>
                      {["License", "Client", "Seats", "Status", "Last seen", ""].map((h) => (
                        <th key={h} className="px-5 py-4 font-medium">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {content.dashboard.rows.map((r) => (
                      <tr key={r.license} className="border-t border-border">
                        <td className="px-5 py-4 font-mono">{r.license}</td>
                        <td className="px-5 py-4">{r.client}</td>
                        <td className="px-5 py-4 text-muted-foreground">{r.seats}</td>
                        <td className="px-5 py-4">
                          <span className={cn("rounded-full border px-3 py-1 text-xs", statusClass[r.status])}>{r.status}</span>
                        </td>
                        <td className="px-5 py-4 text-muted-foreground">{r.lastSeen}</td>
                        <td className="px-5 py-4 text-right">
                          {r.status === "Active" && (
                            <button className="rounded-full border border-destructive/40 px-4 py-1.5 text-xs text-destructive transition-colors hover:bg-destructive/15">
                              Revoke
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </Reveal>
      </div>
    </section>
  );
}
