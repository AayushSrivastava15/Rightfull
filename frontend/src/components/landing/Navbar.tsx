import { useEffect, useState } from "react";
import { Link } from "@tanstack/react-router";
import { ShieldCheck, Menu, Moon, Sun, X, LayoutDashboard } from "lucide-react";
import { content } from "@/content";
import { pill } from "./primitives";
import { cn } from "@/lib/utils";
import { useAuth } from "@/contexts/AuthContext";

export function Navbar() {
  const [open, setOpen] = useState(false);
  const [dark, setDark] = useState(true);
  const { session } = useAuth();

  useEffect(() => {
    setDark(document.documentElement.classList.contains("dark"));
  }, []);

  const toggle = () => {
    const next = !dark;
    setDark(next);
    document.documentElement.classList.toggle("dark", next);
    try {
      localStorage.setItem("theme", next ? "dark" : "light");
    } catch {}
  };

  return (
    <header className="fixed inset-x-0 top-4 z-50 px-4 pointer-events-none">
      <nav
        aria-label="Main navigation"
        className="pointer-events-auto mx-auto flex h-14 max-w-5xl items-center justify-between rounded-full border border-border/80 bg-background/80 px-3 md:px-4 backdrop-blur-xl shadow-lg shadow-black/5 dark:shadow-black/25"
      >
        {/* Left Zone: Logo */}
        <div className="flex items-center shrink-0">
          <Link
            to="/"
            className="flex items-center gap-2.5 font-semibold tracking-tight transition-opacity hover:opacity-90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring rounded-full px-1 py-0.5"
          >
            <span className="grid h-8 w-8 place-items-center rounded-full bg-blue-600/15 text-blue-500 border border-blue-500/20 shadow-xs">
              <ShieldCheck className="h-4.5 w-4.5" aria-hidden="true" />
            </span>
            <span className="text-foreground font-bold tracking-tight text-base">Rightful</span>
          </Link>
        </div>

        {/* Center Zone: Centered Navigation Pill */}
        <div className="hidden lg:flex items-center justify-center flex-1 px-4">
          <ul className="flex items-center gap-1 rounded-full border border-border/70 bg-secondary/50 p-1 backdrop-blur-md">
            {content.nav.map((l) => (
              <li key={l.label}>
                <a
                  href={l.href}
                  className="block rounded-full px-3.5 py-1.5 text-xs font-medium text-muted-foreground transition-all hover:bg-background hover:text-foreground hover:shadow-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  {l.label}
                </a>
              </li>
            ))}
          </ul>
        </div>

        {/* Right Zone: Auth, Theme Toggle, Mobile Toggle */}
        <div className="flex items-center gap-2 shrink-0">
          {session ? (
            <Link
              to="/dashboard"
              className={cn(
                pill({ variant: "primary", size: "sm" }),
                "gap-1.5"
              )}
            >
              <LayoutDashboard className="h-3.5 w-3.5" aria-hidden="true" />
              <span>Dashboard</span>
            </Link>
          ) : (
            <div className="hidden sm:flex items-center gap-1.5">
              <Link
                to="/login"
                className={cn(
                  pill({ variant: "ghost", size: "sm" }),
                  "text-muted-foreground hover:text-foreground"
                )}
              >
                Sign In
              </Link>
              <Link
                to="/signup"
                className={pill({ variant: "primary", size: "sm" })}
              >
                Get Started
              </Link>
            </div>
          )}

          {/* Theme Toggle */}
          <button
            type="button"
            onClick={toggle}
            aria-label={dark ? "Switch to light theme" : "Switch to dark theme"}
            className={cn(
              pill({ variant: "ghost", size: "icon" }),
              "rounded-full border border-border/50 text-muted-foreground hover:text-foreground"
            )}
          >
            {dark ? (
              <Sun className="h-4 w-4 text-amber-400" />
            ) : (
              <Moon className="h-4 w-4 text-slate-700" />
            )}
          </button>

          {/* Mobile / Tablet Menu Button (screens below lg) */}
          <button
            type="button"
            onClick={() => setOpen(!open)}
            aria-label={open ? "Close navigation menu" : "Open navigation menu"}
            aria-expanded={open}
            className={cn(
              pill({ variant: "ghost", size: "icon" }),
              "rounded-full border border-border/50 text-muted-foreground hover:text-foreground lg:hidden"
            )}
          >
            {open ? <X className="h-4 w-4" /> : <Menu className="h-4 w-4" />}
          </button>
        </div>
      </nav>

      {/* Mobile Drawer */}
      {open && (
        <div className="pointer-events-auto mx-auto mt-2 max-w-5xl rounded-3xl border border-border/80 bg-background/95 p-4 backdrop-blur-2xl shadow-2xl lg:hidden animate-in fade-in slide-in-from-top-2 duration-200">
          <ul className="space-y-1">
            {content.nav.map((l) => (
              <li key={l.label}>
                <a
                  href={l.href}
                  onClick={() => setOpen(false)}
                  className="flex items-center rounded-xl px-4 py-2.5 text-sm font-medium text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground"
                >
                  {l.label}
                </a>
              </li>
            ))}
          </ul>
          <div className="mt-3 pt-3 border-t border-border/70 flex flex-col gap-2">
            {session ? (
              <Link
                to="/dashboard"
                onClick={() => setOpen(false)}
                className="flex items-center justify-center gap-2 rounded-xl bg-blue-600 hover:bg-blue-500 py-2.5 text-sm font-semibold text-white shadow-sm"
              >
                <LayoutDashboard className="h-4 w-4" />
                <span>Dashboard</span>
              </Link>
            ) : (
              <>
                <Link
                  to="/login"
                  onClick={() => setOpen(false)}
                  className="block text-center rounded-xl border border-border bg-card/60 py-2.5 text-sm font-medium text-foreground hover:bg-secondary transition-colors"
                >
                  Sign In
                </Link>
                <Link
                  to="/signup"
                  onClick={() => setOpen(false)}
                  className="block text-center rounded-xl bg-blue-600 hover:bg-blue-500 py-2.5 text-sm font-semibold text-white shadow-sm transition-colors"
                >
                  Get Started
                </Link>
              </>
            )}
          </div>
        </div>
      )}
    </header>
  );
}
