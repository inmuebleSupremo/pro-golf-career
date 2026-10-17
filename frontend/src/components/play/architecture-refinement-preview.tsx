"use client";

import { useState } from "react";

import { Hole2d } from "@/components/play/hole-2d";
import previews from "@/lib/play/v5-refinement-preview-fixtures.json";
import type { HoleGeom, Point } from "@/lib/play/hole-geometry";

/** Same seed, biome and hole number in retained V5 and experimental V5-derived geometry. */
export function ArchitectureRefinementPreview() {
  const [aim, setAim] = useState<Point>({ x: 0, y: 190 });
  return <main className="command-centre min-h-screen bg-background p-8 text-foreground">
    <div className="mx-auto max-w-6xl">
      <p className="text-primary text-sm font-semibold tracking-[0.16em] uppercase">Architecture visual checkpoint</p>
      <h1 className="mt-2 text-3xl font-semibold">V5 corridor refinement — before and after</h1>
      <p className="text-muted-foreground mt-3 max-w-3xl">
        Six ordinary backend-generated holes from complete Parkland and Links courses. Each pair uses the same seed,
        biome and hole number. Left: retained V5. Right: experimental V5-derived architecture. Surfaces and hazards
        are canonical; both use the live playable-hole renderer. The experimental version is not active in careers.
      </p>
      <div className="mt-8 space-y-8">
        {previews.map((preview) => <section key={`${preview.biome}-${preview.seed}`}>
          <h2 className="mb-3 text-lg font-semibold">{preview.biome === "PARKLAND" ? "Parkland" : "Links"} · {preview.unseen ? "unseen" : "known"} seed {preview.seed} · Hole {preview.holeNumber} · Par {(preview.after.hole as HoleGeom).par}</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            {(["before", "after"] as const).map((phase) => {
              const item = preview[phase];
              const hole = item.hole as HoleGeom;
              return <figure key={phase} className="border-border bg-surface rounded-xl border p-3">
                <div className="flex items-baseline justify-between gap-2">
                  <h3 className="font-medium">{phase === "before" ? "Retained V5" : "Experimental refinement"}</h3>
                  <span className="text-subtle-foreground text-xs">{hole.length.toFixed(0)} yd</span>
                </div>
                <p className="text-muted-foreground mt-1 text-xs">
                  {item.routing.toLowerCase().replaceAll("_", " ")} · {item.bunkers} bunkers · {item.trees} tree regions · {item.water} water regions
                </p>
                <Hole2d hole={hole} aimPoint={aim} onAimPoint={setAim} presentation="polished" className="mt-3 h-auto w-full rounded-xl" />
              </figure>;
            })}
          </div>
        </section>)}
      </div>
    </div>
  </main>;
}
