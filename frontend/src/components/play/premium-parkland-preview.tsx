"use client";

import { useState } from "react";

import { Hole2d } from "@/components/play/hole-2d";
import corpus from "@/lib/play/principled-architecture-preview-fixtures.json";
import type { HoleGeom, Point } from "@/lib/play/hole-geometry";
import { cn } from "@/lib/utils";

type GeneratedCase = {
  seed: string;
  biome: string;
  unseen: boolean;
  experiment: { generatorVersion: number; holes: readonly { hole: HoleGeom; bunkers: number; trees: number; water: number }[] };
};

const generated = (corpus as readonly GeneratedCase[]).filter((course) =>
  course.biome === "PARKLAND" && course.experiment.generatorVersion === 8);
const cases = [
  { course: generated.find((course) => course.seed === "103")!, holeIndex: 6 },
  { course: generated.find((course) => course.seed === "887")!, holeIndex: 3 },
];
const framingCases = [
  { course: generated.find((course) => course.seed === "103")!, holeIndex: 7 },
  { course: generated.find((course) => course.seed === "103")!, holeIndex: 8 },
];

/** Fixed backend-emitted V8 cases for visual review; actual play uses the live PlayingHole query. */
export function PremiumParklandPreview() {
  const [narrow, setNarrow] = useState(false);
  const [framing, setFraming] = useState(false);
  return <main className="command-centre min-h-screen bg-background px-4 py-8 text-foreground md:px-8">
    <div className={cn("mx-auto", narrow ? "max-w-[390px]" : "max-w-6xl")}>
      <p className="text-primary text-xs font-semibold uppercase tracking-[0.16em]">Development visual checkpoint</p>
      <h1 className="mt-2 font-serif text-3xl">Premium Parkland SVG V1</h1>
      <p className="text-muted-foreground mt-3 max-w-3xl text-sm">
        Backend-generated V8 Parkland holes. Each pair uses identical playable polygons, markers and aim point;
        the premium view shows extra cosmetic scenery around the same course. Tap either view to move the shared target.
      </p>
      <button type="button" aria-pressed={narrow} onClick={() => setNarrow((value) => !value)}
        className="border-border bg-surface text-foreground mt-4 rounded-md border px-3 py-1.5 text-xs">
        {narrow ? "Show desktop comparison" : "Check narrow viewport"}
      </button>
      <button type="button" aria-pressed={framing} onClick={() => setFraming((value) => !value)}
        className="border-border bg-surface text-foreground ml-2 mt-4 rounded-md border px-3 py-1.5 text-xs">
        {framing ? "Hide framing examples" : "Show straight and dogleg framing"}
      </button>
      <div className="mt-8 space-y-10">
        {cases.map(({ course, holeIndex }) => <Comparison key={course.seed} course={course} holeIndex={holeIndex} narrow={narrow} />)}
        {framing ? framingCases.map(({ course, holeIndex }) =>
          <Comparison key={`${course.seed}-${holeIndex}`} course={course} holeIndex={holeIndex} narrow={narrow} />) : null}
      </div>
    </div>
  </main>;
}

function Comparison({ course, holeIndex, narrow }: { course: GeneratedCase; holeIndex: number; narrow: boolean }) {
  const preview = course.experiment.holes[holeIndex];
  const hole = preview.hole;
  const [aim, setAim] = useState<Point>(hole.geometry?.cup ?? { x: 0, y: hole.length });
  return <section>
    <div className="mb-3 flex flex-wrap items-baseline justify-between gap-2">
      <h2 className="font-serif text-xl">Seed {course.seed} · Hole {hole.holeNumber} · Par {hole.par}</h2>
      <span className="text-subtle-foreground text-xs">Generator 8 · {preview.trees} tree regions · {preview.bunkers} bunkers · {preview.water} water regions</span>
    </div>
    <div className={cn("grid gap-4", !narrow && "md:grid-cols-[minmax(240px,1fr)_minmax(0,1.8fr)]")}>
      <Panel title="Current" hole={hole} aim={aim} onAim={setAim} presentation="polished" narrow={narrow} />
      <Panel title="Premium prototype" hole={hole} aim={aim} onAim={setAim} presentation="premium" narrow={narrow} />
    </div>
  </section>;
}

function Panel({ title, hole, aim, onAim, presentation, narrow }: {
  title: string; hole: HoleGeom; aim: Point; onAim: (point: Point) => void;
  presentation: "polished" | "premium"; narrow: boolean;
}) {
  return <figure className={cn("border-border bg-surface rounded-lg border p-3", narrow && presentation === "premium" && "overflow-hidden")}>
    <figcaption className="text-muted-foreground mb-2 text-xs font-medium uppercase tracking-wider">{title}</figcaption>
    <Hole2d hole={hole} aimPoint={aim} onAimPoint={onAim} presentation={presentation}
      className={cn("block h-[min(66vh,700px)] touch-manipulation cursor-crosshair",
        narrow && presentation === "premium" ? "relative left-1/2 w-auto max-w-none -translate-x-1/2" : "mx-auto max-w-full")} />
  </figure>;
}
