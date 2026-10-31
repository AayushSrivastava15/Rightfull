import { createFileRoute } from "@tanstack/react-router";
import { Placeholder } from "@/components/landing/Placeholder";

export const Route = createFileRoute("/contact")({
  head: () => ({
    meta: [
      { title: "Contact us | Rightful" },
      { name: "description", content: "Company licensing enquiries open soon." },
      { property: "og:title", content: "Contact us | Rightful" },
      { property: "og:description", content: "Company licensing enquiries open soon." },
    ],
  }),
  component: () => <Placeholder title="Contact us" body="Company licensing enquiries open soon." />,
});
