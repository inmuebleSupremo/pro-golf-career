"use client";

import { useState } from "react";

import { Hole2d } from "@/components/play/hole-2d";
import corpus from "@/lib/play/principled-architecture-preview-fixtures.json";
import type { HoleGeom, Point } from "@/lib/play/hole-geometry";

type StrategicOption = {
  readonly role: "SAFE" | "PRIMARY" | "AGGRESSIVE";
  readonly aimPoint: Point;
  readonly club: string;
  readonly route: string;
  readonly exposure: string;
};

type PreviewHole = {
  readonly routing: string;
  readonly bunkers: number;
  readonly trees: number;
  readonly water: number;
  readonly meaningfulTurns: number;
  readonly strategy: readonly StrategicOption[];
  readonly hole: HoleGeom;
};

type CourseMetrics = {
  readonly parFourFiveHoles: number;
  readonly straightforwardParFourFive: number;
  readonly meaningfulTurnHoles: number;
  readonly doubleMeaningfulTurnHoles: number;
  readonly bunkerRegions: number;
  readonly groupedBunkerHoles: number;
  readonly waterRegions: number;
  readonly treeRegions: number;
  readonly repeatedStructureHoles: number;
  readonly minimumFairwayWidth: number;
  readonly meanFairwayWidth: number;
};

type PreviewVariant = {
  readonly label: string;
  readonly generatorVersion: number;
  readonly provenance: string;
  readonly metrics: CourseMetrics;
  readonly holes: readonly PreviewHole[];
};

type PreviewCourse = {
  readonly seed: string;
  readonly unseen: boolean;
  readonly biome: "PARKLAND" | "LINKS";
  readonly retained: PreviewVariant;
  readonly experiment: PreviewVariant;
};

const courses = corpus as readonly PreviewCourse[];

/** Development-only review of a full fixed backend corpus, not a gameplay route. */
export function PrincipledArchitecturePreview() {
  const [courseIndex, setCourseIndex] = useState(0);
  const [strategicOnly, setStrategicOnly] = useState(false);
  const course = courses[courseIndex];
  const holes = strategicOnly ? course.experiment.holes.filter((hole) => hole.strategy.length > 1) : course.experiment.holes;

  return <main className="command-centre min-h-screen bg-background p-8 text-foreground">
    <div className="mx-auto max-w-6xl">
      <p className="text-primary text-sm font-semibold tracking-[0.16em] uppercase">Development-only checkpoint</p>
      <h1 className="mt-2 text-3xl font-semibold">Principles-driven strategic architecture</h1>
      <p className="text-muted-foreground mt-3 max-w-3xl">
        Complete same-coordinate comparisons for 12 fixed Parkland/Links courses. Every course offers all 18 holes;
        the final three seeds in each biome are unseen. Retained Generator 7 and the non-persistent experiment share a
        coordinate but are not equivalent generator versions: the latter is a preview-only V7/V5 compiler path.
      </p>
      <div className="border-border bg-surface mt-6 flex flex-wrap items-end gap-4 rounded-xl border p-4">
        <label className="text-muted-foreground flex min-w-56 flex-1 flex-col gap-2 text-sm" htmlFor="preview-course">
          Full fixed corpus course
          <select id="preview-course" value={courseIndex} onChange={(event) => setCourseIndex(Number(event.target.value))}
            className="border-border bg-background text-foreground rounded-[var(--r-sm)] border px-3 py-2">
            {courses.map((item, index) => <option key={`${item.biome}-${item.seed}`} value={index}>
              {item.biome === "PARKLAND" ? "Parkland" : "Links"} · {item.unseen ? "unseen" : "known"} seed {item.seed}
            </option>)}
          </select>
        </label>
        <button type="button" onClick={() => setStrategicOnly((current) => !current)}
          className="border-border bg-background hover:bg-surface-2 rounded-[var(--r-sm)] border px-3 py-2 text-sm font-medium">
          {strategicOnly ? "Show complete 18-hole course" : "Show only decision examples"}
        </button>
        <p className="text-muted-foreground text-sm">
          {course.biome === "PARKLAND" ? "Parkland" : "Links"} · {course.unseen ? "unseen" : "known"} · seed {course.seed} · {holes.length} paired holes
        </p>
      </div>
      <div className="mt-4 grid gap-4 md:grid-cols-2">
        <CourseVariantSummary variant={course.retained} />
        <CourseVariantSummary variant={course.experiment} />
      </div>
      <p className="text-muted-foreground mt-4 max-w-3xl text-sm">
        “Straightforward” counts only par 4/5 holes without a measured material turn. A meaningful turn must occur at a
        credible route distance, change heading by at least 18°, and depart materially from the tee-to-green chord.
        Repeated structures are a coarse diagnostic, not a golf-quality score. Experimental decision examples use the
        shared target evaluator&apos;s literal canonical aim points.
      </p>
      <div className="mt-8 space-y-8">
        {holes.map((preview) => <HoleComparison key={preview.hole.holeNumber} retained={course.retained.holes[preview.hole.holeNumber - 1]}
          experiment={preview} />)}
      </div>
    </div>
  </main>;
}

function CourseVariantSummary({ variant }: {
  readonly variant: PreviewVariant;
}) {
  const metrics = variant.metrics;
  return <section className="border-border bg-surface rounded-xl border p-4">
    <div className="flex items-start justify-between gap-3">
      <div>
        <h2 className="font-medium">{variant.label}</h2>
        <p className="text-muted-foreground mt-1 text-xs">{variant.provenance}</p>
      </div>
    </div>
    <dl className="mt-4 grid grid-cols-2 gap-x-4 gap-y-2 text-xs">
      <Measure label="Straight par 4/5" value={`${metrics.straightforwardParFourFive}/${metrics.parFourFiveHoles}`} />
      <Measure label="Meaningful turns" value={`${metrics.meaningfulTurnHoles} (${metrics.doubleMeaningfulTurnHoles} double)`} />
      <Measure label="Bunker regions" value={`${metrics.bunkerRegions} / ${metrics.groupedBunkerHoles} grouped holes`} />
      <Measure label="Water / tree regions" value={`${metrics.waterRegions} / ${metrics.treeRegions}`} />
      <Measure label="Fairway width" value={`${metrics.minimumFairwayWidth.toFixed(0)}–${metrics.meanFairwayWidth.toFixed(0)} yd min/mean`} />
      <Measure label="Repeated structures" value={String(metrics.repeatedStructureHoles)} />
    </dl>
  </section>;
}

function Measure({ label, value }: { readonly label: string; readonly value: string }) {
  return <div><dt className="text-muted-foreground">{label}</dt><dd className="text-foreground mt-0.5 font-medium">{value}</dd></div>;
}

function HoleComparison({ retained, experiment }: { readonly retained: PreviewHole; readonly experiment: PreviewHole }) {
  return <section>
    <h2 className="mb-3 font-medium">Hole {experiment.hole.holeNumber} · Par {experiment.hole.par} · {experiment.hole.length.toFixed(0)} yd</h2>
    <div className="grid gap-6 md:grid-cols-2">
      <PreviewHoleCard preview={retained} experimental={false} />
      <PreviewHoleCard preview={experiment} experimental />
    </div>
  </section>;
}

function PreviewHoleCard({ preview, experimental }: { readonly preview: PreviewHole; readonly experimental: boolean }) {
  const defaultAim = preview.strategy.find((option) => option.role === "PRIMARY")?.aimPoint ?? preview.hole.geometry?.cup ?? { x: 0, y: preview.hole.length };
  const [aim, setAim] = useState<Point>(defaultAim);
  const decision = preview.strategy.length > 1;

  return <figure className="border-border bg-surface rounded-xl border p-4">
    <div className="flex items-baseline justify-between gap-3">
      <h3 className="font-medium">{experimental ? "Experimental preview" : "Retained Generator 7"}</h3>
      <span className="text-muted-foreground text-xs">{preview.hole.length.toFixed(0)} yd</span>
    </div>
    <p className="text-muted-foreground mt-1 text-xs">
      {preview.routing.toLowerCase().replaceAll("_", " ")} · {preview.meaningfulTurns} measured route turn{preview.meaningfulTurns === 1 ? "" : "s"} · {preview.bunkers} bunkers · {preview.trees} tree regions · {preview.water} water regions
    </p>
    <Hole2d hole={preview.hole} aimPoint={aim} onAimPoint={setAim} presentation="polished" className="mt-4 h-auto w-full rounded-xl" />
    {experimental && decision ? <div className="mt-4 space-y-2">
      <p className="text-primary text-xs font-semibold tracking-[0.12em] uppercase">Shared strategic decisions</p>
      {preview.strategy.map((option) => <button key={option.role} type="button" onClick={() => setAim(option.aimPoint)}
        className="border-border bg-background hover:bg-surface-2 block w-full rounded-[var(--r-sm)] border p-3 text-left">
        <span className="text-foreground text-sm font-medium">{option.role.toLowerCase()} · {option.club}</span>
        <span className="text-muted-foreground mt-1 block text-xs">{option.route} — {option.exposure}</span>
      </button>)}
    </div> : <figcaption className="text-muted-foreground mt-4 text-xs">
      {experimental ? "No materially distinct reachable landing choice from the tee under the fixed review player and calm conditions."
        : "Retained Generator 7 comparison geometry; no preview strategy metadata is inferred for this side."}
    </figcaption>}
  </figure>;
}
