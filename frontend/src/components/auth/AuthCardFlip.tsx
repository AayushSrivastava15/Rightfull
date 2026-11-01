import { useState, useEffect } from "react";
import { Link, useNavigate } from "@tanstack/react-router";
import {
  ShieldCheck,
  Moon,
  Sun,
  Eye,
  EyeOff,
  Loader2,
  AlertCircle,
  CheckCircle,
  ArrowLeft,
} from "lucide-react";
import { useReducedMotion } from "motion/react";
import { useAuth } from "@/contexts/AuthContext";
import { useTheme } from "@/contexts/ThemeContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

interface AuthCardFlipProps {
  initialMode?: "login" | "signup";
}

function WaveBackground() {
  const lines = Array.from({ length: 14 }, (_, i) => i);
  return (
    <div
      aria-hidden="true"
      className="pointer-events-none absolute inset-0 overflow-hidden"
      style={{
        maskImage:
          "radial-gradient(ellipse 70% 60% at 50% 45%, black 20%, transparent 75%)",
        WebkitMaskImage:
          "radial-gradient(ellipse 70% 60% at 50% 45%, black 20%, transparent 75%)",
      }}
    >
      <svg
        className="animate-wave absolute -left-[10%] top-0 h-full w-[120%] opacity-40 dark:opacity-60"
        viewBox="0 0 1200 800"
        preserveAspectRatio="none"
      >
        <defs>
          <linearGradient id="auth-wl" x1="0" x2="1">
            <stop offset="0" stopColor="var(--brand-glow)" stopOpacity="0" />
            <stop offset="0.5" stopColor="var(--brand)" stopOpacity="0.8" />
            <stop offset="1" stopColor="var(--brand-glow)" stopOpacity="0" />
          </linearGradient>
        </defs>
        {lines.map((i) => (
          <path
            key={i}
            d={`M -50 ${700 - i * 26} C 300 ${520 - i * 32}, 650 ${760 - i * 20}, 1250 ${120 + i * 16}`}
            fill="none"
            stroke="url(#auth-wl)"
            strokeWidth={i % 4 === 0 ? 1.4 : 0.7}
            opacity={0.25 + (i % 5) * 0.12}
          />
        ))}
      </svg>
      <div className="absolute left-1/2 top-1/3 h-72 w-[36rem] -translate-x-1/2 rounded-full bg-blue-600/15 blur-[120px] dark:bg-brand/20" />
    </div>
  );
}

export function AuthCardFlip({ initialMode = "login" }: AuthCardFlipProps) {
  const navigate = useNavigate();
  const reduceMotion = useReducedMotion();
  const { signInWithEmail, signUpWithEmail, signInWithOAuth, error: authContextError } = useAuth();

  const [mode, setMode] = useState<"login" | "signup">(initialMode);
  const [dark, setDark] = useState(true);

  // Sign In form state
  const [loginEmail, setLoginEmail] = useState("");
  const [loginPassword, setLoginPassword] = useState("");
  const [showLoginPassword, setShowLoginPassword] = useState(false);
  const [loginLoading, setLoginLoading] = useState(false);
  const [loginError, setLoginError] = useState<string | null>(null);

  // Sign Up form state
  const [signupName, setSignupName] = useState("");
  const [signupEmail, setSignupEmail] = useState("");
  const [signupPassword, setSignupPassword] = useState("");
  const [signupConfirmPassword, setSignupConfirmPassword] = useState("");
  const [showSignupPassword, setShowSignupPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [signupLoading, setSignupLoading] = useState(false);
  const [signupError, setSignupError] = useState<string | null>(null);
  const [signupSuccess, setSignupSuccess] = useState<string | null>(null);

  // Initialize theme and title
  useEffect(() => {
    setDark(document.documentElement.classList.contains("dark"));
    document.title =
      mode === "signup"
        ? "Create Account | Rightful"
        : "Sign In | Rightful";
  }, [mode]);

  // Sync mode with browser back/forward buttons
  useEffect(() => {
    const handlePopState = () => {
      const path = window.location.pathname;
      if (path === "/signup" || path === "/sign-up") {
        setMode("signup");
        setLoginError(null);
      } else if (path === "/login" || path === "/sign-in") {
        setMode("login");
        setSignupError(null);
      }
    };

    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  let themeContext: ReturnType<typeof useTheme> | null = null;
  try {
    // eslint-disable-next-line react-hooks/rules-of-hooks
    themeContext = useTheme();
  } catch {
    // Rendered outside ThemeProvider (e.g. in test)
  }

  useEffect(() => {
    if (themeContext) {
      setDark(themeContext.resolvedTheme === "dark");
    } else {
      setDark(document.documentElement.classList.contains("dark"));
    }
  }, [themeContext?.resolvedTheme]);

  const toggleTheme = () => {
    const next = !dark;
    setDark(next);
    if (themeContext) {
      themeContext.setTheme(next ? "dark" : "light");
    } else {
      document.documentElement.classList.toggle("dark", next);
      document.body?.classList.toggle("dark", next);
      try {
        localStorage.setItem("rightfull-theme", next ? "dark" : "light");
      } catch {}
    }
  };

  const flipTo = (target: "login" | "signup") => {
    if (mode === target) return;
    setMode(target);
    setLoginError(null);
    setSignupError(null);
    setSignupSuccess(null);

    const targetUrl = target === "signup" ? "/signup" : "/login";
    window.history.pushState({ mode: target }, "", targetUrl);
  };

  // Sign In submission
  const handleLoginSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoginError(null);

    if (!loginEmail.trim() || !loginPassword) {
      setLoginError("Please enter both email and password.");
      return;
    }

    setLoginLoading(true);
    try {
      const { error } = await signInWithEmail(loginEmail.trim(), loginPassword);
      if (error) {
        setLoginError(error.message);
      } else {
        navigate({ to: "/dashboard" });
      }
    } catch (err: unknown) {
      setLoginError(err instanceof Error ? err.message : "Sign in failed.");
    } finally {
      setLoginLoading(false);
    }
  };

  // Sign Up submission
  const handleSignupSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSignupError(null);
    setSignupSuccess(null);

    if (!signupEmail.trim() || !signupPassword) {
      setSignupError("Please fill in all required fields.");
      return;
    }

    if (signupPassword.length < 8) {
      setSignupError("Password must be at least 8 characters long.");
      return;
    }

    if (signupPassword !== signupConfirmPassword) {
      setSignupError("Passwords do not match.");
      return;
    }

    setSignupLoading(true);
    try {
      const { error } = await signUpWithEmail(
        signupEmail.trim(),
        signupPassword,
        signupName.trim()
      );
      if (error) {
        setSignupError(error.message);
      } else {
        setSignupSuccess("Account created! Provisioning workspace...");
        setTimeout(() => {
          navigate({ to: "/dashboard" });
        }, 1200);
      }
    } catch (err: unknown) {
      setSignupError(err instanceof Error ? err.message : "Registration failed.");
    } finally {
      setSignupLoading(false);
    }
  };

  // OAuth submission
  const handleOAuth = async (provider: "google" | "github") => {
    setLoginError(null);
    setSignupError(null);
    const activeLoading = mode === "signup" ? setSignupLoading : setLoginLoading;
    const activeError = mode === "signup" ? setSignupError : setLoginError;

    activeLoading(true);
    try {
      const { error } = await signInWithOAuth(provider);
      if (error) {
        activeError(error.message);
        activeLoading(false);
      }
    } catch (err: unknown) {
      activeError(err instanceof Error ? err.message : "OAuth authentication failed.");
      activeLoading(false);
    }
  };

  const isFlipped = mode === "signup";

  return (
    <div className="relative flex min-h-screen flex-col items-center justify-between overflow-x-hidden bg-background text-foreground transition-colors p-4 sm:p-6">
      <WaveBackground />

      {/* Top Header Controls */}
      <header className="relative z-10 flex w-full max-w-5xl items-center justify-between py-2">
        <Link
          to="/"
          className="inline-flex items-center gap-2 rounded-full border border-border/80 bg-background/60 px-3.5 py-1.5 text-xs font-medium text-muted-foreground backdrop-blur-md transition-all hover:bg-secondary hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          <ArrowLeft className="h-3.5 w-3.5" />
          <span>Back to Home</span>
        </Link>

        <div className="flex items-center gap-2">
          <Link
            to="/"
            className="flex items-center gap-2 font-bold tracking-tight text-foreground transition-opacity hover:opacity-90"
          >
            <span className="grid h-7 w-7 place-items-center rounded-full bg-blue-600/15 text-blue-500 border border-blue-500/20">
              <ShieldCheck className="h-4 w-4" />
            </span>
            <span className="hidden sm:inline">Rightful</span>
          </Link>

          <button
            type="button"
            onClick={toggleTheme}
            aria-label={dark ? "Switch to light theme" : "Switch to dark theme"}
            className="grid h-8 w-8 place-items-center rounded-full border border-border/70 bg-background/60 text-muted-foreground backdrop-blur-md transition-colors hover:bg-secondary hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring ml-2"
          >
            {dark ? (
              <Sun className="h-3.5 w-3.5 text-amber-400" />
            ) : (
              <Moon className="h-3.5 w-3.5 text-slate-700" />
            )}
          </button>
        </div>
      </header>

      {/* Main 3D Card Area */}
      <main className="relative z-10 my-auto flex w-full flex-col items-center justify-center py-6">
        <div className="perspective-container w-full max-w-[440px] min-h-[640px]">
          <div
            className={cn(
              "preserve-3d relative w-full min-h-[640px] transition-transform duration-700 ease-[cubic-bezier(0.4,0,0.2,1)]",
              reduceMotion && "transition-none",
              !reduceMotion && isFlipped && "rotate-y-180"
            )}
          >
            {/* FRONT FACE: SIGN IN */}
            <div
              className={cn(
                "w-full rounded-3xl border border-border/80 bg-card/90 p-6 sm:p-8 shadow-2xl backdrop-blur-xl transition-opacity duration-300",
                !reduceMotion && "backface-hidden",
                reduceMotion &&
                  (isFlipped
                    ? "opacity-0 pointer-events-none absolute inset-0"
                    : "opacity-100 relative"),
                isFlipped && !reduceMotion && "pointer-events-none select-none"
              )}
              inert={isFlipped ? true : undefined}
              aria-hidden={isFlipped}
            >
              <div className="space-y-1.5 text-center">
                <div className="mx-auto mb-3 grid h-12 w-12 place-items-center rounded-2xl bg-blue-600/15 text-blue-500 border border-blue-500/20 shadow-xs">
                  <ShieldCheck className="h-6 w-6" />
                </div>
                <h1 className="text-2xl font-bold tracking-tight text-foreground">
                  Sign In to Rightful
                </h1>
                <p className="text-xs text-muted-foreground">
                  Enter your credentials to access your licensing workspaces
                </p>
              </div>

              {/* Error Alert */}
              {(loginError || (mode === "login" && authContextError)) && (
                <div className="mt-4 flex items-center gap-2 rounded-xl border border-red-500/30 bg-red-500/10 p-3 text-xs text-red-500 dark:text-red-400">
                  <AlertCircle className="h-4 w-4 shrink-0" />
                  <span>{loginError || authContextError}</span>
                </div>
              )}

              {/* OAuth Providers */}
              <div className="mt-5 grid grid-cols-2 gap-3">
                <Button
                  type="button"
                  variant="outline"
                  className="w-full gap-2 rounded-xl border-border/80 bg-background/50 hover:bg-secondary text-xs font-medium"
                  disabled={loginLoading}
                  onClick={() => handleOAuth("google")}
                >
                  <svg className="h-4 w-4" viewBox="0 0 24 24">
                    <path
                      fill="currentColor"
                      d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                    />
                    <path
                      fill="currentColor"
                      d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                    />
                    <path
                      fill="currentColor"
                      d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
                    />
                    <path
                      fill="currentColor"
                      d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
                    />
                  </svg>
                  Google
                </Button>

                <Button
                  type="button"
                  variant="outline"
                  className="w-full gap-2 rounded-xl border-border/80 bg-background/50 hover:bg-secondary text-xs font-medium"
                  disabled={loginLoading}
                  onClick={() => handleOAuth("github")}
                >
                  <svg className="h-4 w-4" fill="currentColor" viewBox="0 0 24 24">
                    <path
                      fillRule="evenodd"
                      clipRule="evenodd"
                      d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"
                    />
                  </svg>
                  GitHub
                </Button>
              </div>

              {/* Divider */}
              <div className="relative my-4 flex items-center justify-center">
                <div className="w-full border-t border-border" />
                <span className="absolute bg-card px-2 text-[11px] uppercase tracking-wider text-muted-foreground">
                  Or continue with email
                </span>
              </div>

              {/* Form */}
              <form onSubmit={handleLoginSubmit} className="space-y-3.5">
                <div className="space-y-1">
                  <label className="text-xs font-medium text-foreground">
                    Email address
                  </label>
                  <Input
                    type="email"
                    placeholder="name@company.com"
                    value={loginEmail}
                    onChange={(e) => setLoginEmail(e.target.value)}
                    disabled={loginLoading}
                    autoComplete="email"
                    required
                    className="h-10 rounded-xl bg-background/50 border-border/80 focus-visible:ring-ring text-sm"
                  />
                </div>

                <div className="space-y-1">
                  <div className="flex items-center justify-between">
                    <label className="text-xs font-medium text-foreground">
                      Password
                    </label>
                  </div>
                  <div className="relative">
                    <Input
                      type={showLoginPassword ? "text" : "password"}
                      placeholder="••••••••"
                      value={loginPassword}
                      onChange={(e) => setLoginPassword(e.target.value)}
                      disabled={loginLoading}
                      autoComplete="current-password"
                      required
                      className="h-10 rounded-xl bg-background/50 border-border/80 pr-10 focus-visible:ring-ring text-sm"
                    />
                    <button
                      type="button"
                      className="absolute right-3 top-2.5 text-muted-foreground hover:text-foreground transition-colors"
                      onClick={() => setShowLoginPassword(!showLoginPassword)}
                      tabIndex={-1}
                      aria-label={showLoginPassword ? "Hide sign in password" : "Show sign in password"}
                    >
                      {showLoginPassword ? (
                        <EyeOff className="h-4 w-4" />
                      ) : (
                        <Eye className="h-4 w-4" />
                      )}
                    </button>
                  </div>
                </div>

                <Button
                  type="submit"
                  className="w-full h-10 rounded-xl bg-blue-600 hover:bg-blue-500 font-semibold text-white shadow-sm shadow-blue-500/20 transition-all mt-2"
                  disabled={loginLoading}
                >
                  {loginLoading ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Signing In...
                    </>
                  ) : (
                    "Sign In"
                  )}
                </Button>
              </form>

              {/* Flip to Sign Up Footer */}
              <div className="mt-5 border-t border-border/60 pt-4 text-center text-xs text-muted-foreground">
                Don&apos;t have an account?{" "}
                <button
                  type="button"
                  onClick={() => flipTo("signup")}
                  className="font-semibold text-blue-500 hover:text-blue-400 hover:underline transition-colors ml-1 cursor-pointer"
                >
                  Sign Up
                </button>
              </div>
            </div>

            {/* BACK FACE: SIGN UP */}
            <div
              className={cn(
                "w-full rounded-3xl border border-border/80 bg-card/90 p-6 sm:p-8 shadow-2xl backdrop-blur-xl transition-opacity duration-300 absolute inset-0",
                !reduceMotion && "backface-hidden rotate-y-180",
                reduceMotion &&
                  (!isFlipped
                    ? "opacity-0 pointer-events-none"
                    : "opacity-100 relative"),
                !isFlipped && !reduceMotion && "pointer-events-none select-none"
              )}
              inert={!isFlipped ? true : undefined}
              aria-hidden={!isFlipped}
            >
              <div className="space-y-1.5 text-center">
                <div className="mx-auto mb-3 grid h-12 w-12 place-items-center rounded-2xl bg-blue-600/15 text-blue-500 border border-blue-500/20 shadow-xs">
                  <ShieldCheck className="h-6 w-6" />
                </div>
                <h1 className="text-2xl font-bold tracking-tight text-foreground">
                  Create an Account
                </h1>
                <p className="text-xs text-muted-foreground">
                  Provision your software licensing workspace in seconds
                </p>
              </div>

              {/* Error & Success Alerts */}
              {(signupError || (mode === "signup" && authContextError)) && (
                <div className="mt-3 flex items-center gap-2 rounded-xl border border-red-500/30 bg-red-500/10 p-2.5 text-xs text-red-500 dark:text-red-400">
                  <AlertCircle className="h-4 w-4 shrink-0" />
                  <span>{signupError || authContextError}</span>
                </div>
              )}

              {signupSuccess && (
                <div className="mt-3 flex items-center gap-2 rounded-xl border border-emerald-500/30 bg-emerald-500/10 p-2.5 text-xs text-emerald-500 dark:text-emerald-400">
                  <CheckCircle className="h-4 w-4 shrink-0" />
                  <span>{signupSuccess}</span>
                </div>
              )}

              {/* OAuth Providers */}
              <div className="mt-4 grid grid-cols-2 gap-3">
                <Button
                  type="button"
                  variant="outline"
                  className="w-full gap-2 rounded-xl border-border/80 bg-background/50 hover:bg-secondary text-xs font-medium"
                  disabled={signupLoading}
                  onClick={() => handleOAuth("google")}
                >
                  <svg className="h-4 w-4" viewBox="0 0 24 24">
                    <path
                      fill="currentColor"
                      d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                    />
                    <path
                      fill="currentColor"
                      d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                    />
                    <path
                      fill="currentColor"
                      d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
                    />
                    <path
                      fill="currentColor"
                      d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
                    />
                  </svg>
                  Google
                </Button>

                <Button
                  type="button"
                  variant="outline"
                  className="w-full gap-2 rounded-xl border-border/80 bg-background/50 hover:bg-secondary text-xs font-medium"
                  disabled={signupLoading}
                  onClick={() => handleOAuth("github")}
                >
                  <svg className="h-4 w-4" fill="currentColor" viewBox="0 0 24 24">
                    <path
                      fillRule="evenodd"
                      clipRule="evenodd"
                      d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"
                    />
                  </svg>
                  GitHub
                </Button>
              </div>

              {/* Divider */}
              <div className="relative my-3 flex items-center justify-center">
                <div className="w-full border-t border-border" />
                <span className="absolute bg-card px-2 text-[11px] uppercase tracking-wider text-muted-foreground">
                  Or register with email
                </span>
              </div>

              {/* Form */}
              <form onSubmit={handleSignupSubmit} className="space-y-2.5">
                <div className="space-y-1">
                  <label className="text-xs font-medium text-foreground">
                    Full Name
                  </label>
                  <Input
                    type="text"
                    placeholder="Jane Doe"
                    value={signupName}
                    onChange={(e) => setSignupName(e.target.value)}
                    disabled={signupLoading}
                    autoComplete="name"
                    className="h-9 rounded-xl bg-background/50 border-border/80 focus-visible:ring-ring text-xs"
                  />
                </div>

                <div className="space-y-1">
                  <label className="text-xs font-medium text-foreground">
                    Email address
                  </label>
                  <Input
                    type="email"
                    placeholder="name@company.com"
                    value={signupEmail}
                    onChange={(e) => setSignupEmail(e.target.value)}
                    disabled={signupLoading}
                    autoComplete="email"
                    required
                    className="h-9 rounded-xl bg-background/50 border-border/80 focus-visible:ring-ring text-xs"
                  />
                </div>

                <div className="grid grid-cols-2 gap-2">
                  <div className="space-y-1">
                    <label className="text-xs font-medium text-foreground">
                      Password
                    </label>
                    <div className="relative">
                      <Input
                        type={showSignupPassword ? "text" : "password"}
                        placeholder="Min 8 chars"
                        value={signupPassword}
                        onChange={(e) => setSignupPassword(e.target.value)}
                        disabled={signupLoading}
                        autoComplete="new-password"
                        required
                        className="h-9 rounded-xl bg-background/50 border-border/80 pr-8 focus-visible:ring-ring text-xs"
                      />
                      <button
                        type="button"
                        className="absolute right-2 top-2 text-muted-foreground hover:text-foreground transition-colors"
                        onClick={() => setShowSignupPassword(!showSignupPassword)}
                        tabIndex={-1}
                        aria-label={showSignupPassword ? "Hide sign up password" : "Show sign up password"}
                      >
                        {showSignupPassword ? (
                          <EyeOff className="h-3.5 w-3.5" />
                        ) : (
                          <Eye className="h-3.5 w-3.5" />
                        )}
                      </button>
                    </div>
                  </div>

                  <div className="space-y-1">
                    <label className="text-xs font-medium text-foreground">
                      Confirm
                    </label>
                    <div className="relative">
                      <Input
                        type={showConfirmPassword ? "text" : "password"}
                        placeholder="Repeat"
                        value={signupConfirmPassword}
                        onChange={(e) => setSignupConfirmPassword(e.target.value)}
                        disabled={signupLoading}
                        autoComplete="new-password"
                        required
                        className="h-9 rounded-xl bg-background/50 border-border/80 pr-8 focus-visible:ring-ring text-xs"
                      />
                      <button
                        type="button"
                        className="absolute right-2 top-2 text-muted-foreground hover:text-foreground transition-colors"
                        onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                        tabIndex={-1}
                        aria-label={showConfirmPassword ? "Hide confirm password" : "Show confirm password"}
                      >
                        {showConfirmPassword ? (
                          <EyeOff className="h-3.5 w-3.5" />
                        ) : (
                          <Eye className="h-3.5 w-3.5" />
                        )}
                      </button>
                    </div>
                  </div>
                </div>

                <Button
                  type="submit"
                  className="w-full h-10 rounded-xl bg-blue-600 hover:bg-blue-500 font-semibold text-white shadow-sm shadow-blue-500/20 transition-all mt-2"
                  disabled={signupLoading}
                >
                  {signupLoading ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Creating Account...
                    </>
                  ) : (
                    "Create Account"
                  )}
                </Button>
              </form>

              {/* Flip to Sign In Footer */}
              <div className="mt-4 border-t border-border/60 pt-3 text-center text-xs text-muted-foreground">
                Already have an account?{" "}
                <button
                  type="button"
                  onClick={() => flipTo("login")}
                  className="font-semibold text-blue-500 hover:text-blue-400 hover:underline transition-colors ml-1 cursor-pointer"
                >
                  Sign In
                </button>
              </div>
            </div>
          </div>
        </div>
      </main>

      {/* Footer copyright */}
      <footer className="relative z-10 py-2 text-center text-[11px] text-muted-foreground">
        &copy; {new Date().getFullYear()} Rightful Platform. All rights reserved.
      </footer>
    </div>
  );
}
