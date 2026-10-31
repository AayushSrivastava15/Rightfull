import { createFileRoute } from "@tanstack/react-router";
import { Placeholder } from "@/components/landing/Placeholder";

export const Route = createFileRoute("/docs")({
  head: () => ({
    meta: [
      { title: "Docs | Rightful" },
      { name: "description", content: "Documentation is on its way." },
      { property: "og:title", content: "Docs | Rightful" },
      { property: "og:description", content: "Documentation is on its way." },
    ],
  }),
  component: () => <Placeholder title="Docs" body="Documentation is on its way." />,
});
