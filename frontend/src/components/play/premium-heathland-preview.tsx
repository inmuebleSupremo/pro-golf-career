"use client";

import { useState } from "react";
import Image from "next/image";
import { Hole2d } from "@/components/play/hole-2d";
import course from "@/lib/play/premium-heathland-course-fixture.json";
import type { HoleGeom, Point } from "@/lib/play/hole-geometry";

const holes = course.holes.map((item) => item.hole as HoleGeom);

export function PremiumHeathlandPreview() {
  const [selected, setSelected] = useState(4);
  const [narrow, setNarrow] = useState(false);
  const [showContact, setShowContact] = useState(false);
  const [aim, setAim] = useState<Point>({ x: 0, y: 190 });
  const active = holes[selected - 1];
  return <main className="command-centre min-h-screen bg-background p-4 text-foreground md:p-8">
    <div className="mx-auto max-w-7xl">
      <p className="text-primary text-sm font-semibold uppercase tracking-wider">Generator 8 · Heathland seed {course.seed}</p>
      <h1 className="mt-2 text-3xl font-semibold">Premium Heathland visual evidence</h1>
      <p className="text-muted-foreground mt-2 text-sm">Actual generated Heathland geometry. Select a hole and aim on either presentation.</p>
      <div className="mt-4 flex flex-wrap gap-2">
        <button type="button" className="border-border rounded border px-3 py-1 text-sm" onClick={() => setNarrow(!narrow)}>
          {narrow ? "Narrow stage" : "Desktop stage"}
        </button>
        <label className="text-muted-foreground self-center text-sm" htmlFor="heathland-review-hole">Hole</label>
        <select id="heathland-review-hole" className="border-border bg-surface rounded border px-2 py-1 text-sm"
          value={selected} onChange={(event) => setSelected(Number(event.target.value))}>
          {holes.map((hole) => <option key={hole.holeNumber} value={hole.holeNumber}>
            {hole.holeNumber} · Par {hole.par} · {Math.round(hole.length)} yd
          </option>)}
        </select>
        <span className="text-muted-foreground self-center text-sm">Hole {selected} · Par {active.par} · {Math.round(active.length)} yards</span>
      </div>
      <section className="mt-4 grid gap-4 lg:grid-cols-2" aria-label="Approved vector and generated Heathland comparison">
        <figure className="border-border bg-surface overflow-hidden rounded-lg border p-3">
          <figcaption className="mb-2 text-sm font-medium">Approved optimized SVG · unchanged</figcaption>
          <Image src="/dev/premium-heathland-source" unoptimized loading="eager" width={1600} height={900}
            alt="Approved Premium Heathland vector" className="block aspect-video w-full" />
        </figure>
        <figure className="border-border bg-surface overflow-hidden rounded-lg border p-3">
          <figcaption className="mb-2 text-sm font-medium">Generated Heathland hole · integrated artwork</figcaption>
          <Hole2d hole={active} presentation="premium" previewAspect={16 / 9} className="block aspect-video w-full" />
        </figure>
      </section>
      <section className="border-border bg-surface mt-4 overflow-hidden rounded-lg border p-3" aria-label="Full-size generated Heathland hole">
        <h2 className="mb-2 text-sm font-medium">Full-size generated hole {active.holeNumber} · par {active.par}</h2>
        <Hole2d hole={active} presentation="premium" previewAspect={16 / 9}
          aimPoint={aim} onAimPoint={setAim} className="block aspect-video w-full touch-manipulation cursor-crosshair" />
      </section>
      <section className="mt-4 grid gap-4 lg:grid-cols-2" aria-label="Heathland presentation comparison">
        {(["polished", "premium"] as const).map((presentation) => <figure key={presentation}
          className="border-border bg-surface overflow-hidden rounded-lg border p-3">
          <figcaption className="mb-2 text-sm font-medium">{presentation === "premium" ? "Premium Heathland" : "Current Heathland"}</figcaption>
          <Hole2d hole={active} presentation={presentation} aimPoint={aim} onAimPoint={setAim}
            previewAspect={presentation === "premium" ? narrow ? .68 : 2.16 : undefined}
            className={presentation === "premium"
              ? narrow
                ? "relative left-1/2 block h-[min(62vh,600px)] w-[min(68vw,300px)] max-w-none -translate-x-1/2 touch-manipulation cursor-crosshair"
                : "block h-[min(62vh,600px)] w-full touch-manipulation cursor-crosshair"
              : "mx-auto block h-[min(62vh,600px)] w-auto max-w-full touch-manipulation cursor-crosshair"} />
        </figure>)}
      </section>
      <section className="mt-8" aria-label="18-hole Premium Heathland contact sheet">
        <div className="mb-3 flex items-center gap-3">
          <h2 className="text-xl font-semibold">18-hole contact sheet</h2>
          <button type="button" className="border-border rounded border px-3 py-1 text-sm"
            onClick={() => setShowContact(!showContact)}>{showContact ? "Hide sheet" : "Show sheet"}</button>
        </div>
        {showContact ? <div className="grid grid-cols-2 gap-2 md:grid-cols-3 lg:grid-cols-6">
          {holes.map((hole) => <button key={hole.holeNumber} type="button" onClick={() => setSelected(hole.holeNumber)}
            className="border-border bg-surface overflow-hidden rounded-md border p-1 text-left">
            <span className="block px-1 py-1 text-xs">{hole.holeNumber}. Par {hole.par} · {Math.round(hole.length)} yd</span>
            <Hole2d hole={hole} presentation="premium" previewAspect={.8} className="pointer-events-none block h-52 w-full" />
          </button>)}
        </div> : null}
      </section>
    </div>
  </main>;
}
