"use client";

import { WindDisplay, type EffectiveWindDisplay } from "./wind-display";
import { canonicalRenderModel, type CanonicalPlayingGeometry } from "../../lib/play/canonical-geometry";

const previewGeometry: CanonicalPlayingGeometry = {
  tee: { x: 0, y: 0 },
  cup: { x: 0, y: 100 },
  playableBoundary: [{ x: -20, y: 0 }, { x: 20, y: 0 }, { x: 20, y: 110 }, { x: -20, y: 110 }],
  regions: [],
};

const previewModel = canonicalRenderModel(previewGeometry);

const fixtures: readonly { readonly name: string; readonly description: string; readonly wind: EffectiveWindDisplay }[] = [
  { name: "Tailwind", description: "+Y · airflow up the hole", wind: { x: 0, y: 5, magnitude: 5, unit: "EFFECTIVE_YARDS" } },
  { name: "Headwind", description: "-Y · airflow down the hole", wind: { x: 0, y: -5, magnitude: 5, unit: "EFFECTIVE_YARDS" } },
  { name: "Right crosswind", description: "+X · airflow screen-right", wind: { x: 5, y: 0, magnitude: 5, unit: "EFFECTIVE_YARDS" } },
  { name: "Left crosswind", description: "-X · airflow screen-left", wind: { x: -5, y: 0, magnitude: 5, unit: "EFFECTIVE_YARDS" } },
];

/** Development-only visual fixture: no API calls, event state, or resolver authority. */
export function WindDisplayPreview() {
  return <main className="bg-background text-foreground min-h-screen p-10">
    <div className="mx-auto max-w-3xl space-y-10">
      <header className="space-y-2">
        <p className="text-info text-xs font-bold tracking-[0.12em] uppercase">Development-only preview</p>
        <h1 className="text-2xl font-bold">Effective wind display</h1>
        <p className="text-muted-foreground max-w-xl text-sm">Fixed server-shaped fixtures verify display direction only. They do not change live weather, shots, or player state.</p>
      </header>
      <section className="grid gap-4 sm:grid-cols-2" aria-label="Wind display fixtures">
        {fixtures.map((fixture) => <article key={fixture.name} className="border-border bg-surface rounded-[var(--r)] border p-[22px]">
          <h2 className="text-base font-bold">{fixture.name}</h2>
          <p className="text-muted-foreground mt-1 text-xs">{fixture.description}</p>
          <svg viewBox="0 0 220 54" className="bg-surface-2 mt-4 w-full rounded-[var(--r-sm)]" role="img" aria-label={`${fixture.name} wind display`}>
            <WindDisplay wind={fixture.wind} model={previewModel} tee={previewGeometry.tee} />
          </svg>
        </article>)}
      </section>
    </div>
  </main>;
}
