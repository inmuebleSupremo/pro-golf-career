"use client";

import { useState } from "react";

import { Hole2d } from "@/components/play/hole-2d";
import previews from "@/lib/play/links-v5-preview-fixtures.json";
import type { HoleGeom, Point } from "@/lib/play/hole-geometry";

/** Deterministic backend V5 geometry rendered by the normal playable-hole component. */
export function LinksRenderPreview() {
  const [aim, setAim] = useState<Point>({ x: 0, y: 190 });
  return <main className="command-centre min-h-screen bg-background p-8 text-foreground">
    <div className="mx-auto max-w-6xl">
      <p className="text-primary text-sm font-semibold tracking-[0.16em] uppercase">Links visual checkpoint</p>
      <h1 className="mt-2 text-3xl font-semibold">V5 Links — exposed turf and fescue</h1>
      <p className="text-muted-foreground mt-3 max-w-3xl">
        Four ordinary backend-generated Links holes, including two unseen seeds. Every bunker, water feature,
        tree region and playable surface comes from canonical geometry. The dune and fescue materials change
        appearance only. Click a hole to exercise the normal aiming interaction.
      </p>
      <div className="mt-8 grid gap-6 sm:grid-cols-2">
        {previews.map((preview) => {
          const hole = preview.hole as HoleGeom;
          return <figure key={preview.seed} className="border-border bg-surface rounded-xl border p-4 shadow-lg">
            <div className="flex items-baseline justify-between gap-3">
              <h2 className="font-medium">{preview.unseen ? "Unseen" : "Known"} seed {preview.seed} · Hole {hole.holeNumber}</h2>
              <span className="text-subtle-foreground text-xs">Par {hole.par} · {hole.length.toFixed(0)} yd</span>
            </div>
            <p className="text-muted-foreground mt-1 text-xs">
              {preview.routing.toLowerCase().replaceAll("_", " ")} · {preview.bunkers} bunkers · {preview.trees} tree regions · {preview.water} water regions
            </p>
            <Hole2d hole={hole} aimPoint={aim} onAimPoint={setAim} presentation="polished" className="mt-4 h-auto w-full rounded-xl" />
            <figcaption className="text-muted-foreground mt-3 text-sm">Canonical layout seed {hole.layoutSeed}</figcaption>
          </figure>;
        })}
      </div>
    </div>
  </main>;
}
