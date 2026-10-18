"use client";

import { useState } from "react";

import { Hole2d } from "@/components/play/hole-2d";
import { PARKLAND_V5_AFTER_PREVIEWS } from "@/lib/play/parkland-v5-after-green-preview-fixtures";
import { PARKLAND_V5_PREVIEWS, type ParklandV5Preview } from "@/lib/play/parkland-v5-preview-fixtures";
import type { Point } from "@/lib/play/hole-geometry";

/** Development-only visual checkpoint using fixed, backend-emitted V5 canonical geometry. */
export function ParklandRenderPreview() {
  const [aim, setAim] = useState<Point>({ x: 0, y: 220 });
  const beforeBySeed = new Map(PARKLAND_V5_PREVIEWS.map((preview) => [preview.seed, preview]));

  return <main className="command-centre min-h-screen bg-background p-8 text-foreground">
    <div className="mx-auto max-w-6xl">
      <p className="text-primary text-sm font-semibold tracking-[0.16em] uppercase">Green-complex visual checkpoint</p>
      <h1 className="mt-2 text-3xl font-semibold">V5 Parkland — same seed, embedded green complex</h1>
      <p className="text-muted-foreground mt-3 max-w-3xl">
        Each row compares the original V5 endpoint geometry with the corrected V5 geometry from the same coordinate,
        hole and layout seed. Every visible surface is a backend-emitted canonical polygon; the renderer only projects
        and frames those polygons. Click either rendering to retain the normal canonical aiming interaction.
      </p>
      <div className="mt-8 space-y-10">
        {PARKLAND_V5_AFTER_PREVIEWS.map((after) => <GreenComparison key={after.seed} before={beforeBySeed.get(after.seed)!}
          after={after} aim={aim} onAim={setAim} />)}
      </div>
      <p className="text-muted-foreground mt-8 max-w-3xl text-sm">
        The correction stops the fairway before a widening short-grass approach, carries rough and the playable
        landscape around and beyond the green, and frames every canonical terrain vertex. The third row remains an
        unseen seed. No saved career is altered.
      </p>
    </div>
  </main>;
}

function GreenComparison({ before, after, aim, onAim }: {
  before: ParklandV5Preview;
  after: ParklandV5Preview;
  aim: Point;
  onAim: (point: Point) => void;
}) {
  return <section>
    <div className="mb-3 flex flex-wrap items-baseline justify-between gap-2">
      <h2 className="font-medium">{after.unseen ? "Unseen" : "Known"} seed — {after.routing.toLowerCase().replaceAll("_", " ")}</h2>
      <span className="text-subtle-foreground text-xs">{after.unseen ? "unseen" : "known"} seed {after.seed} · Par {after.hole.par} · {after.routing.toLowerCase().replaceAll("_", " ")}</span>
    </div>
    <div className="grid gap-6 md:grid-cols-2">
      <PreviewPanel title="Before — terminal green" preview={before} aim={aim} onAim={onAim} />
      <PreviewPanel title="After — embedded complex" preview={after} aim={aim} onAim={onAim} />
    </div>
  </section>;
}

function PreviewPanel({ title, preview, aim, onAim }: {
  title: string;
  preview: ParklandV5Preview;
  aim: Point;
  onAim: (point: Point) => void;
}) {
  const details = [
    `${preview.bunkers} bunker ${preview.bunkers === 1 ? "region" : "regions"}`,
    `${preview.trees} tree ${preview.trees === 1 ? "region" : "regions"}`,
    preview.water > 0 ? `${preview.water} water ${preview.water === 1 ? "feature" : "features"}` : "no water selected",
  ];
  return <figure className="border-border bg-surface rounded-xl border p-4 shadow-lg">
    <div className="flex items-baseline justify-between gap-3">
      <h3 className="font-medium">{title}</h3>
      <span className="text-subtle-foreground text-xs">{preview.hole.length.toFixed(0)}y</span>
    </div>
    <p className="text-muted-foreground mt-1 text-xs">{details.join(" · ")}</p>
    <Hole2d hole={preview.hole} aimPoint={aim} onAimPoint={onAim} presentation="polished"
      className="mt-4 h-auto w-full rounded-xl" />
    <figcaption className="text-muted-foreground mt-3 text-sm">
      Canonical V5 hole {preview.hole.holeNumber}; layout seed {preview.hole.layoutSeed}.
    </figcaption>
  </figure>;
}

