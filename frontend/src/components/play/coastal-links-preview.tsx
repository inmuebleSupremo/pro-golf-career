"use client";

import { useState } from "react";
import { Hole2d } from "@/components/play/hole-2d";
import course from "@/lib/play/coastal-links-course-fixture.json";
import type { HoleGeom } from "@/lib/play/hole-geometry";

/** Development evidence: the normal production hole renderer with a complete backend-generated course. */
export function CoastalLinksPreview({ initialHole = 7 }: { initialHole?: number }) {
  const [selected, setSelected] = useState(initialHole);
  const item = course.holes[selected - 1];
  const hole = item.hole as HoleGeom;
  return <main className="command-centre min-h-screen bg-background text-foreground">
    <header className="bg-surface border-border flex flex-wrap items-center gap-3 border-b px-4 py-2">
      <span className="text-sm font-semibold">Generated Links · seed {course.seed} · hole {selected} · par {hole.par}</span>
      <span className="text-muted-foreground text-xs">{item.coastal ? "Open coast" : "Inland"} · {item.routing.toLowerCase().replaceAll("_", " ")}</span>
      <nav className="ml-auto flex flex-wrap gap-1" aria-label="Generated holes">
        {course.holes.map((entry, index) => <button type="button" key={index}
          onClick={() => setSelected(index + 1)}
          className={`border-border rounded border px-2 py-1 text-xs ${selected === index + 1 ? "bg-primary text-primary-foreground" : "bg-background"}`}
          aria-label={`Hole ${index + 1}${entry.coastal ? " coastal" : " inland"}`}>
          {index + 1}
        </button>)}
      </nav>
    </header>
    <Hole2d hole={hole} presentation="premium" previewAspect={16 / 9}
      className="block h-[calc(100vh-50px)] min-h-[600px] w-full" />
  </main>;
}
