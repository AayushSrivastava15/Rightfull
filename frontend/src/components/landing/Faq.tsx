import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion";
import { content } from "@/content";
import { Reveal, SectionTitle } from "./primitives";

export function Faq() {
  return (
    <section className="section" aria-labelledby="faq-title">
      <div className="container-page">
        <SectionTitle id="faq-title" eyebrow="FAQ" parts={["Questions, ", "answered."]} />
        <Reveal className="mx-auto max-w-3xl">
          <Accordion type="single" collapsible className="space-y-4">
            {content.faq.map((f, i) => (
              <AccordionItem key={f.q} value={`q${i}`} className="rounded-3xl border border-border bg-card/60 px-6">
                <AccordionTrigger className="py-6 text-base hover:no-underline">{f.q}</AccordionTrigger>
                <AccordionContent className="pb-6 text-base leading-relaxed text-muted-foreground">{f.a}</AccordionContent>
              </AccordionItem>
            ))}
          </Accordion>
        </Reveal>
      </div>
    </section>
  );
}
