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
  { course: generated.find((course) => course.seed === "103")!, holeIndex: 0, layout: "straight" },
  { course: generated.find((course) => course.seed === "887")!, holeIndex: 0, layout: "straight" },
  { course: generated.find((course) => course.seed === "103")!, holeIndex: 6, layout: "dogleg" },
  { course: generated.find((course) => course.seed === "103")!, holeIndex: 1, layout: "dogleg" },
  { course: generated.find((course) => course.seed === "103")!, holeIndex: 8, layout: "building review" },
  { course: generated.find((course) => course.seed === "271")!, holeIndex: 6, layout: "reverse neighbour route" },
  { course: generated.find((course) => course.seed === "271")!, holeIndex: 12, layout: "connected diagonal neighbour" },
];

/** Fixed backend-emitted V8 cases for visual review; actual play uses the live PlayingHole query. */
export function PremiumParklandPreview({ initialNarrow = false, unlitAudit = false, noMottleAudit = false,
  noPathAudit = false, noNetworkAudit = false }: {
  initialNarrow?: boolean; unlitAudit?: boolean; noMottleAudit?: boolean; noPathAudit?: boolean;
  noNetworkAudit?: boolean;
}) {
  const [narrow, setNarrow] = useState(initialNarrow);
  const [before, setBefore] = useState<"polished" | "premium-v1">("premium-v1");
  return <main data-audit-no-mottle={noMottleAudit || undefined} data-audit-no-path={noPathAudit || undefined}
    className="command-centre min-h-screen bg-background px-4 py-8 text-foreground md:px-8">
    {noMottleAudit ? <style>{'[data-audit-no-mottle] svg [fill*="premium-rough-mottle"] { display: none; }'}</style> : null}
    {noPathAudit ? <style>{'[data-audit-no-path] [data-property-path] { display: none; }'}</style> : null}
    {noNetworkAudit ? <style>{'[data-property-path-kind="neighbor"], [data-property-path-kind="junction"] { display: none; }'}</style> : null}
    <div className={cn("mx-auto", narrow ? "max-w-[390px]" : "max-w-6xl")}>
      <p className="text-primary text-xs font-semibold uppercase tracking-[0.16em]">Development visual checkpoint</p>
      <h1 className="mt-2 font-serif text-3xl">Premium Parkland SVG V2</h1>
      <p className="text-muted-foreground mt-3 max-w-3xl text-sm">
        Backend-generated V8 Parkland holes. Each pair uses identical playable polygons, markers and aim point.
        Tap the active corridor in either view to move the shared target.
      </p>
      {unlitAudit ? <p className="text-muted-foreground mt-2 text-xs">Pre-atmosphere diagnostic: stage lighting hidden in both views{noMottleAudit ? "; V1 rough mottle hidden for isolation" : ""}.</p> : null}
      <button type="button" aria-pressed={narrow} onClick={() => setNarrow((value) => !value)}
        className="border-border bg-surface text-foreground mt-4 rounded-md border px-3 py-1.5 text-xs">
        {narrow ? "Show desktop comparison" : "Check narrow viewport"}
      </button>
      <button type="button" aria-pressed={before === "polished"} onClick={() => setBefore((value) => value === "polished" ? "premium-v1" : "polished")}
        className="border-border bg-surface text-foreground ml-2 mt-4 rounded-md border px-3 py-1.5 text-xs">
        {before === "polished" ? "Compare with V1" : "Compare with Current"}
      </button>
      <div className="mt-8 space-y-10">
        {cases.map(({ course, holeIndex, layout }) =>
          <Comparison key={`${course.seed}-${holeIndex}`} course={course} holeIndex={holeIndex} layout={layout}
            before={before} narrow={narrow} unlitAudit={unlitAudit} />)}
      </div>
    </div>
  </main>;
}

function Comparison({ course, holeIndex, layout, before, narrow, unlitAudit }: {
  course: GeneratedCase; holeIndex: number; layout: string; before: "polished" | "premium-v1"; narrow: boolean;
  unlitAudit: boolean;
}) {
  const preview = course.experiment.holes[holeIndex];
  const hole = preview.hole;
  const [aim, setAim] = useState<Point>(hole.geometry?.cup ?? { x: 0, y: hole.length });
  return <section id={`seed-${course.seed}-hole-${hole.holeNumber}`}>
    <div className="mb-3 flex flex-wrap items-baseline justify-between gap-2">
      <h2 className="font-serif text-xl">Seed {course.seed} · Hole {hole.holeNumber} · Par {hole.par} · {layout}</h2>
      <span className="text-subtle-foreground text-xs">Generator 8 · {preview.trees} tree regions · {preview.bunkers} bunkers · {preview.water} water regions</span>
    </div>
    <div className="grid gap-4">
      <Panel title={before === "polished" ? "Current" : "V1 checkpoint"} hole={hole} aim={aim} onAim={setAim}
        presentation={before} narrow={narrow} unlitAudit={unlitAudit} />
      <Panel title="Full-stage V2" hole={hole} aim={aim} onAim={setAim} presentation="premium"
        narrow={narrow} unlitAudit={unlitAudit} />
    </div>
  </section>;
}

function Panel({ title, hole, aim, onAim, presentation, narrow, unlitAudit }: {
  title: string; hole: HoleGeom; aim: Point; onAim: (point: Point) => void;
  presentation: "polished" | "premium-v1" | "premium"; narrow: boolean; unlitAudit: boolean;
}) {
  return <figure className="border-border bg-surface overflow-hidden rounded-lg border p-3">
    <figcaption className="text-muted-foreground mb-2 text-xs font-medium uppercase tracking-wider">{title}</figcaption>
    <Hole2d hole={hole} aimPoint={aim} onAimPoint={onAim} presentation={presentation}
      unlitAudit={unlitAudit}
      previewAspect={presentation === "premium" ? narrow ? .68 : 2.16 : undefined}
      className={cn("block h-[min(58vh,620px)] touch-manipulation cursor-crosshair",
        presentation === "premium" && narrow ? "relative left-1/2 w-auto max-w-none -translate-x-1/2"
          : presentation === "premium" ? "w-full" : "mx-auto max-w-full w-auto")} />
  </figure>;
}
