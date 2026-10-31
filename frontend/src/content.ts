export const content = {
  nav: [
    { label: "Home", href: "#top" },
    { label: "How it works", href: "#how" },
    { label: "Features", href: "#features" },
    { label: "Pricing", href: "#pricing" },
    { label: "Docs", href: "/docs" },
  ],
  hero: {
    badge: "Free for individual developers",
    line1: "License your software.",
    line2: "Revoke in one click.",
    sub: "A free, open-source ticket office for your projects. Give clients a license, see who uses it, and cancel access the moment the agreement is broken.",
    primary: "Get started free",
    secondary: "View docs",
    note: "No credit card · Works with Node, Python, Java, or plain REST",
  },
  compat: ["Node.js", "Python", "Java", "TypeScript", "REST API", "Docker"],
  how: {
    title: ["Simple enough for a ", "five-year-old."],
    steps: [
      { title: "Create a product and a license key", body: "Think of it as printing a ticket for your client." },
      { title: "Your client's app asks the Hub: is this ticket still good?", body: "One tiny check when the app starts up." },
      { title: "Client breaks the rules? Click revoke. The app stops.", body: "The next check says no, and that's it." },
    ],
  },
  features: {
    title: ["Everything you need, ", "nothing you don't."],
    items: [
      { key: "revoke", title: "Instant revoke", body: "One click and the license is dead everywhere on the next check." },
      { key: "seats", title: "Individual and company licenses", body: "Free single-seat licenses, or company plans with seats an admin can assign." },
      { key: "tokens", title: "Signed tokens with grace period", body: "Ed25519-signed tokens work offline for a grace period, so our downtime never locks clients out." },
      { key: "log", title: "Activation log", body: "See every machine, IP and timestamp that used a key." },
      { key: "sdk", title: "SDK or plain API", body: "Use our SDKs or a single HTTP call. Your choice." },
      { key: "oss", title: "Open source, free to start", body: "Self-host it or use ours. MIT licensed." },
    ],
  },
  integration: {
    title: ["Add it in ", "five lines."],
    tabs: [
      {
        id: "sdk",
        label: "SDK",
        code: `import { checkLicense } from "@rightfull/sdk";

const ok = await checkLicense("LIC-XXXX");
if (!ok) process.exit(1);`,
      },
      {
        id: "rest",
        label: "REST API",
        code: `const res = await fetch("https://api.rightfull.com/api/v1/validation/heartbeat", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ key: "LIC-XXXX" }),
});
const { valid } = await res.json();`,
      },
      {
        id: "file",
        label: "Drop-in file",
        code: `// license.js: copy this one file into your project
export async function checkLicense(key) {
  const r = await fetch("https://hub.example.com/validate?key=" + key);
  return (await r.json()).valid === true;
}`,
      },
    ],
  },
  dashboard: {
    title: ["Your licenses, ", "at a glance."],
    rows: [
      { license: "LIC-8F2A", client: "Acme Studio", seats: "5 / 10", status: "Active", lastSeen: "2 min ago" },
      { license: "LIC-19C4", client: "Nordwind GmbH", seats: "1 / 1", status: "Revoked", lastSeen: "3 days ago" },
      { license: "LIC-77B0", client: "Pixel & Co", seats: "3 / 3", status: "Active", lastSeen: "1 hour ago" },
      { license: "LIC-0D5E", client: "Freelance Joe", seats: "1 / 1", status: "Expired", lastSeen: "2 weeks ago" },
      { license: "LIC-A1F9", client: "Brightline Labs", seats: "12 / 20", status: "Active", lastSeen: "Just now" },
    ],
  },
  pricing: {
    title: ["Free to start. ", "Fair to scale."],
    plans: [
      { name: "Individual", price: "Free", features: ["1 seat", "1 product", "Activation log", "Community support"], cta: "Get started free", href: "/signup", highlight: false },
      { name: "Company", price: "Paid", features: ["Many seats", "Seat management by company admin", "Multiple products", "Priority support"], cta: "Contact us", href: "/contact", highlight: true },
    ],
  },
  faq: [
    { q: "Do I need to install an SDK?", a: "No. The SDKs are a convenience. A single HTTP request to /validate is all you need, from any language." },
    { q: "What if your server goes down?", a: "Validations return a signed Ed25519 token that your app can verify offline. A grace period keeps paying clients running while the Hub is unreachable." },
    { q: "Can I revoke one employee only?", a: "Yes. On company licenses each seat is separate, so you or the company admin can free or revoke a single seat." },
    { q: "Is it really free?", a: "Individual licenses are free forever, and the project is open source so you can self-host. Company licenses with seats are paid." },
    { q: "Can clients bypass it?", a: "Honestly, any client-side check can be patched by a determined person. The strongest setup combines server-side checks for key features with a signed agreement, so bypassing it is both hard and a clear breach of contract." },
  ],
  cta: {
    title: ["Stop hoping clients ", "play fair."],
    button: "Get started free",
    waitlist: "Or join the waitlist for company licenses",
  },
  footer: [
    { label: "Docs", href: "/docs" },
    { label: "GitHub", href: "https://github.com" },
    { label: "Contact", href: "/contact" },
  ],
};
