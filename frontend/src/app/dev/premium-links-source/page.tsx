/** Exact, unmodified approved SVG served from public/dev for visual baseline review. */
export default function PremiumLinksSourcePage() {
  if (process.env.NODE_ENV !== "development") return null;
  return <main className="bg-background min-h-screen p-4 text-foreground">
    <h1 className="mb-4 text-xl font-semibold">Approved Premium Links vector source</h1>
    <object data="/dev/premium-links-approved.svg" type="image/svg+xml"
      aria-label="Approved Premium Links SVG" className="mx-auto block aspect-video w-full max-w-[1600px]">
      Approved Premium Links SVG
    </object>
  </main>;
}
