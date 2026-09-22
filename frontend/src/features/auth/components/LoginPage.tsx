import { useState, type FormEvent } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { motion } from "framer-motion";
import {
  ShieldCheck,
  Loader2,
  AlertCircle,
  Lock,
  Mail,
  User,
  Badge as BadgeIcon,
  Fingerprint,
  ScanText,
  Sparkles,
  Landmark,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input, Label } from "@/components/ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useAuth } from "@/contexts/AuthContext";
import { PipelineIllustration } from "./PipelineIllustration";

const FEATURE_BADGES = [
  { icon: ShieldCheck, label: "Rule Engine is the sole compliance authority" },
  { icon: ScanText, label: "OCR + Vision AI fusion" },
  { icon: Fingerprint, label: "SHA-256 hashed evidence" },
  { icon: Sparkles, label: "Explainable AI Copilot" },
] as const;

/**
 * Full-screen entry point — the first thing anyone, including SIH judges,
 * sees. Both tabs call real backend auth endpoints (`/auth/login`,
 * `/auth/signup`) via `useAuth()`; there is no other authentication path
 * in the app.
 */
export function LoginPage() {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  const [tab, setTab] = useState("signin");

  if (isAuthenticated) {
    const from = (location.state as { from?: string })?.from ?? "/";
    return <Navigate to={from} replace />;
  }

  return (
    // `h-dvh` (dynamic viewport height), not `h-screen` (100vh) — real mobile
    // browsers render 100vh as if the address bar were already hidden, so it
    // overshoots the actually-visible area. Combined with `overflow-hidden`
    // below, that pushed the Sign In button past the true visible fold with
    // no way to scroll to it — reachable only in "Desktop Site" mode, which
    // forces a fixed viewport and sidesteps the dynamic-toolbar calculation
    // entirely (exactly the symptom reported: works in Desktop Site, not in
    // normal mobile view). `dvh` is recalculated as the browser chrome
    // shows/hides, so it always matches what's really on screen.
    <div className="grid h-dvh w-screen grid-cols-1 overflow-hidden bg-background lg:grid-cols-[58%_42%]">
      {/* Left — brand, pipeline illustration, feature badges */}
      <div className="relative hidden flex-col justify-between overflow-hidden bg-[#05060f] px-12 py-12 lg:flex xl:px-20">
        <div className="bg-gradient-landing pointer-events-none absolute inset-0" />
        <motion.div
          className="pointer-events-none absolute -left-24 top-10 size-72 rounded-full opacity-30 blur-3xl"
          style={{ backgroundColor: "var(--color-ai-blue)" }}
          animate={{ y: [0, 24, 0], x: [0, 12, 0] }}
          transition={{ duration: 9, repeat: Infinity, ease: "easeInOut" }}
        />
        <motion.div
          className="pointer-events-none absolute bottom-0 right-0 size-96 rounded-full opacity-20 blur-3xl"
          style={{ backgroundColor: "var(--color-ai-emerald)" }}
          animate={{ y: [0, -20, 0], x: [0, -16, 0] }}
          transition={{ duration: 11, repeat: Infinity, ease: "easeInOut", delay: 1 }}
        />
        <motion.div
          className="pointer-events-none absolute right-1/4 top-1/3 size-64 rounded-full opacity-20 blur-3xl"
          style={{ backgroundColor: "var(--color-ai-violet)" }}
          animate={{ y: [0, 16, 0] }}
          transition={{ duration: 8, repeat: Infinity, ease: "easeInOut", delay: 0.5 }}
        />
        <div
          className="pointer-events-none absolute inset-0 opacity-[0.05]"
          style={{
            backgroundImage:
              "linear-gradient(#fff 1px, transparent 1px), linear-gradient(90deg, #fff 1px, transparent 1px)",
            backgroundSize: "56px 56px",
          }}
        />

        <motion.div
          initial={{ opacity: 0, y: -12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="relative z-10 flex items-center gap-3"
        >
          <div className="bg-gradient-ai flex size-11 items-center justify-center rounded-xl text-white shadow-glow-ai">
            <ShieldCheck className="size-6" />
          </div>
          <span className="text-2xl font-semibold tracking-tight text-white">Nirikshan AI</span>
        </motion.div>

        <div className="relative z-10 max-w-xl space-y-8">
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.15, duration: 0.5 }}
            className="space-y-4"
          >
            <span className="inline-flex items-center gap-1.5 rounded-full border border-white/15 bg-white/5 px-3 py-1 text-[11px] font-medium text-white/70">
              <Landmark className="size-3" />
              Government of India · Legal Metrology
            </span>
            <h1 className="text-4xl font-semibold leading-[1.1] tracking-tight text-white xl:text-5xl">
              Government-grade AI inspection,
              <br />
              built to be explained.
            </h1>
            <p className="max-w-md text-sm leading-relaxed text-white/60">
              Photograph a product label and get a fully auditable compliance verdict — OCR and
              Vision AI read it, a versioned Rule Engine decides, and every violation ships with
              cryptographically hashed photographic evidence.
            </p>
          </motion.div>

          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.35 }}>
            <PipelineIllustration />
          </motion.div>

          <div className="flex flex-wrap gap-2">
            {FEATURE_BADGES.map((badge, index) => (
              <motion.span
                key={badge.label}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.9 + index * 0.08 }}
                className="inline-flex items-center gap-1.5 rounded-full border border-white/10 bg-white/[0.06] px-3 py-1.5 text-[11px] font-medium text-white/75 backdrop-blur-sm"
              >
                <badge.icon className="size-3 text-cyan-300" />
                {badge.label}
              </motion.span>
            ))}
          </div>
        </div>

        <p className="relative z-10 text-[11px] text-white/35">
          © {new Date().getFullYear()} Nirikshan AI — Legal Metrology Inspection System
        </p>
      </div>

      {/* Right — auth card */}
      <div className="relative flex items-center justify-center overflow-y-auto bg-background px-6 py-10">
        <div className="pointer-events-none absolute inset-0 lg:hidden">
          <div className="bg-gradient-landing absolute inset-0" />
        </div>

        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, ease: [0.16, 1, 0.3, 1] }}
          className="relative z-10 w-full max-w-md"
        >
          <div className="mb-8 flex flex-col items-center gap-3 text-center lg:hidden">
            <div className="bg-gradient-ai flex size-12 items-center justify-center rounded-2xl text-white shadow-glow-ai">
              <ShieldCheck className="size-6" />
            </div>
            <h1 className="text-xl font-semibold tracking-tight text-foreground">Nirikshan AI</h1>
          </div>

          <div className="glass glass-border rounded-2xl border p-8 shadow-lg sm:p-10">
            <div className="mb-6 space-y-1">
              <h2 className="text-2xl font-semibold tracking-tight text-foreground">
                {tab === "signin" ? "Welcome back" : "Create your account"}
              </h2>
              <p className="text-sm text-muted-foreground">
                {tab === "signin"
                  ? "Sign in to the Officer Portal to run real, live inspections."
                  : "Register as a Legal Metrology inspector."}
              </p>
            </div>

            <Tabs value={tab} onValueChange={setTab}>
              <TabsList className="mb-6 grid w-full grid-cols-2 h-10">
                <TabsTrigger value="signin" className="text-sm">Sign In</TabsTrigger>
                <TabsTrigger value="register" className="text-sm">Register</TabsTrigger>
              </TabsList>

              <TabsContent value="signin" className="mt-0">
                <SignInForm />
              </TabsContent>
              <TabsContent value="register" className="mt-0">
                <RegisterForm onSuccess={() => setTab("signin")} />
              </TabsContent>
            </Tabs>
          </div>

          <p className="mt-6 text-center text-[11px] text-faint-foreground">
            Rule Engine is the sole authority on compliance decisions.
          </p>
        </motion.div>
      </div>
    </div>
  );
}

/** Extracts the backend's real validation/auth message (e.g. a 400 "email: Email must be a valid email address" or a 401 "Invalid email or password") instead of masking every failure behind one generic string — the two mean very different things to a user typing on a phone keyboard. */
function backendErrorMessage(err: unknown): string {
  const data = (err as { response?: { data?: { message?: string; errors?: string[] } } })?.response?.data;
  if (data?.errors?.length) return data.errors[0];
  return data?.message ?? "Something went wrong — please try again.";
}

function SignInForm() {
  const { login } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      // Trimmed client-side too (the backend now also trims) — mobile
      // keyboards/autofill commonly append a trailing space to email
      // fields, which would otherwise silently turn into a confusing
      // "invalid email or password" instead of actually signing in.
      await login(email.trim(), password);
    } catch (err) {
      setError(backendErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      <div className="space-y-1.5">
        <Label htmlFor="signin-email">Email</Label>
        <div className="relative">
          <Mail className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-faint-foreground" />
          <Input
            id="signin-email"
            type="email"
            required
            autoComplete="username"
            autoCapitalize="none"
            autoCorrect="off"
            spellCheck={false}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="officer@lmis.gov.in"
            className="h-11 pl-10 text-sm"
          />
        </div>
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="signin-password">Password</Label>
        <div className="relative">
          <Lock className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-faint-foreground" />
          <Input
            id="signin-password"
            type="password"
            required
            autoComplete="current-password"
            autoCapitalize="none"
            autoCorrect="off"
            spellCheck={false}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="h-11 pl-10 text-sm"
          />
        </div>
      </div>

      {error && (
        <motion.p
          initial={{ opacity: 0, y: -4 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex items-center gap-1.5 rounded-md bg-critical-soft px-3 py-2 text-xs text-critical-foreground"
        >
          <AlertCircle className="size-3.5 shrink-0" />
          {error}
        </motion.p>
      )}

      <Button type="submit" variant="ai" className="w-full" size="lg" disabled={isSubmitting}>
        {isSubmitting && <Loader2 className="size-4 animate-spin" />}
        Sign In
      </Button>
    </form>
  );
}

function RegisterForm({ onSuccess }: { onSuccess: () => void }) {
  const { register } = useAuth();
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [employeeCode, setEmployeeCode] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      await register({
        fullName: fullName.trim(),
        email: email.trim(),
        password,
        employeeCode: employeeCode.trim() || undefined,
      });
      onSuccess();
    } catch (err) {
      setError(backendErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="space-y-1.5">
        <Label htmlFor="register-name">Full Name</Label>
        <div className="relative">
          <User className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-faint-foreground" />
          <Input
            id="register-name"
            required
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            placeholder="Asha Verma"
            className="h-11 pl-10 text-sm"
          />
        </div>
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="register-email">Email</Label>
        <div className="relative">
          <Mail className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-faint-foreground" />
          <Input
            id="register-email"
            type="email"
            required
            autoComplete="username"
            autoCapitalize="none"
            autoCorrect="off"
            spellCheck={false}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="officer@lmis.gov.in"
            className="h-11 pl-10 text-sm"
          />
        </div>
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="register-employee-code">Employee Code (optional)</Label>
        <div className="relative">
          <BadgeIcon className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-faint-foreground" />
          <Input
            id="register-employee-code"
            value={employeeCode}
            onChange={(e) => setEmployeeCode(e.target.value)}
            placeholder="LM-2026-0142"
            className="h-11 pl-10 text-sm"
          />
        </div>
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="register-password">Password</Label>
        <div className="relative">
          <Lock className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-faint-foreground" />
          <Input
            id="register-password"
            type="password"
            required
            autoComplete="new-password"
            autoCapitalize="none"
            autoCorrect="off"
            spellCheck={false}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="h-11 pl-10 text-sm"
          />
        </div>
        <p className="text-[11px] text-faint-foreground">
          At least 8 characters, with an uppercase letter, a lowercase letter, a digit, and a special character.
        </p>
      </div>

      {error && (
        <motion.p
          initial={{ opacity: 0, y: -4 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex items-center gap-1.5 rounded-md bg-critical-soft px-3 py-2 text-xs text-critical-foreground"
        >
          <AlertCircle className="size-3.5 shrink-0" />
          {error}
        </motion.p>
      )}

      <Button type="submit" variant="ai" className="w-full" size="lg" disabled={isSubmitting}>
        {isSubmitting && <Loader2 className="size-4 animate-spin" />}
        Create Account
      </Button>
    </form>
  );
}
