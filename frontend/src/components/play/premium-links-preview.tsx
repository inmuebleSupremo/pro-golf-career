"use client";

import { useState } from "react";
import { Hole2d } from "@/components/play/hole-2d";
import course from "@/lib/play/premium-links-course-fixture.json";
import type { HoleGeom, Point } from "@/lib/play/hole-geometry";

const holes = course.holes.map((item) => ({ ...item, hole: {
  ...item.hole, courseFamily: "LINKS", canonicalBiome: "LINKS", courseGeneratorVersion: 8,
  canonicalGeometrySource: "GENERATOR_8_PRINCIPLED_ARCHITECTURE",
} as HoleGeom }));

export function PremiumLinksPreview() {
  const [selected, setSelected] = useState(4);
  const [narrow, setNarrow] = useState(false);
  const [aim, setAim] = useState<Point>({ x: 0, y: 190 });
  const active = holes[selected - 1].hole;
  const modeAspect = narrow ? .68 : 2.16;
  return <main className="command-centre min-h-screen bg-background p-4 text-foreground md:p-8">
    <div className="mx-auto max-w-7xl">
      <p className="text-primary text-sm font-semibold uppercase tracking-wider">Generator 8 · Links seed {course.seed}</p>
      <h1 className="mt-2 text-3xl font-semibold">Premium Links visual evidence</h1>
      <p className="text-muted-foreground mt-2 text-sm">One complete generated course. Select a hole to inspect the full-size comparison and aim on either presentation.</p>
      <div className="mt-4 flex flex-wrap gap-2">
        <button type="button" className="border-border rounded border px-3 py-1 text-sm" onClick={() => setNarrow(!narrow)}>
          {narrow ? "Narrow stage" : "Desktop stage"}
        </button>
        <span className="text-muted-foreground self-center text-sm">Hole {selected} · Par {active.par} · {holes[selected - 1].routing.toLowerCase()}</span>
      </div>
      <section className="mt-4 grid gap-4 lg:grid-cols-2" aria-label="Approved vector and generated Links comparison">
        <figure className="border-border bg-surface overflow-hidden rounded-lg border p-3">
          <figcaption className="mb-2 text-sm font-medium">Approved vector source · unchanged</figcaption>
          <object data="/dev/premium-links-approved.svg" type="image/svg+xml"
            aria-label="Exact approved Premium Links vector" className="block aspect-video w-full">
            Approved Premium Links SVG
          </object>
        </figure>
        <figure className="border-border bg-surface overflow-hidden rounded-lg border p-3">
          <figcaption className="mb-2 text-sm font-medium">Generated Links hole · integrated artwork</figcaption>
          <Hole2d hole={active} presentation="premium" previewAspect={16 / 9}
            className="block aspect-video w-full" />
        </figure>
      </section>
      <section className="mt-4 grid gap-4 lg:grid-cols-2" aria-label="Links presentation comparison">
        {(["polished", "premium"] as const).map((presentation) => <figure key={presentation} className="border-border bg-surface overflow-hidden rounded-lg border p-3">
          <figcaption className="mb-2 text-sm font-medium">{presentation === "premium" ? "Premium Links" : "Original Links"}</figcaption>
          <Hole2d hole={active} aimPoint={aim} onAimPoint={setAim} presentation={presentation}
            previewAspect={presentation === "premium" ? modeAspect : undefined}
            className={presentation === "premium"
              ? narrow
                ? "relative left-1/2 block h-[min(62vh,600px)] w-[min(68vw,300px)] max-w-none -translate-x-1/2 touch-manipulation cursor-crosshair"
                : "block h-[min(62vh,600px)] w-full touch-manipulation cursor-crosshair"
              : "mx-auto block h-[min(62vh,600px)] w-auto max-w-full touch-manipulation cursor-crosshair"} />
        </figure>)}
      </section>
      <section className="mt-8" aria-label="18-hole Premium Links contact sheet">
        <h2 className="mb-3 text-xl font-semibold">18-hole contact sheet</h2>
        <div className="grid grid-cols-2 gap-2 md:grid-cols-3 lg:grid-cols-6">
          {holes.map(({ hole, routing }) => <button key={hole.holeNumber} type="button" onClick={() => setSelected(hole.holeNumber)}
            className="border-border bg-surface overflow-hidden rounded-md border p-1 text-left">
            <span className="block px-1 py-1 text-xs">{hole.holeNumber}. Par {hole.par} · {routing.toLowerCase()}</span>
            <Hole2d hole={hole} presentation="premium" previewAspect={.8} className="pointer-events-none block h-52 w-full" />
          </button>)}
        </div>
      </section>
    </div>
  </main>;
}
