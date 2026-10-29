import { notFound } from "next/navigation";

import { PlayMode } from "@/components/play/play-mode";
import type { Situation } from "@/components/play/play-shared";
import type { HoleGeom } from "@/lib/play/hole-geometry";
import familyCases from "@/lib/play/course-family-preview-fixtures.json";
import linksCourse from "@/lib/play/premium-links-course-fixture.json";
import heathlandCourse from "@/lib/play/premium-heathland-course-fixture.json";
import desertCourse from "@/lib/play/premium-desert-course-fixture.json";
import puttingStates from "@/lib/play/premium-putting-states.json";

type Family = "parkland" | "woodland" | "links" | "heathland" | "desert";
const courses: Record<Family, HoleGeom> = {
  parkland: (familyCases as { identity: string; hole: HoleGeom }[]).find((sample) => sample.identity === "PARKLAND")!.hole,
  woodland: (familyCases as { identity: string; hole: HoleGeom }[]).find((sample) => sample.identity === "WOODLAND")!.hole,
  links: { ...linksCourse.holes[0].hole, courseFamily: "LINKS", canonicalBiome: "LINKS",
    courseGeneratorVersion: 8, canonicalGeometrySource: "GENERATOR_8_PRINCIPLED_ARCHITECTURE" } as HoleGeom,
  heathland: heathlandCourse.holes[0].hole as HoleGeom,
  desert: desertCourse.holes[0].hole as HoleGeom,
};

/** Backend-generated fixtures, rendered by the same PlayMode, stage, HUD and dock as an active round. */
export default async function Page({ searchParams }: {
  searchParams: Promise<{ family?: string; mode?: string; layout?: string }>;
}) {
  if (process.env.NODE_ENV !== "development") notFound();
  const params = await searchParams;
  const family = (params.family ?? "parkland") as Family;
  if (!(family in courses)) notFound();
  const putting = params.mode === "putting";
  const base = courses[family];
  const puttingFamily = family === "woodland" ? "parkland" : family;
  const states = puttingStates[puttingFamily];
  const hole = putting ? { ...base, ...states["60"], ball: states["23"].ball } as HoleGeom : base;
  const ball = hole.ball?.position ?? hole.geometry!.tee;
  const cup = hole.geometry!.cup;
  const distance = Math.hypot(cup.x - ball.x, cup.y - ball.y);
  const target = { x: ball.x + (cup.x - ball.x) * .78, y: ball.y + (cup.y - ball.y) * .78 };
  const situation: Situation = {
    holeNumber: hole.holeNumber, par: hole.par, shotNumber: putting ? 3 : 1, strokesThisHole: putting ? 2 : 0,
    distanceToPin: putting ? distance * 3 : distance, lie: putting ? "GREEN" : "TEE", pinLateral: hole.pinLateral ?? 0,
    minReach: 0, maxReach: 400, shotRevision: "fixture-evidence", aimEnvelope: null,
    guidance: { safe: target, primary: target, aggressive: cup,
      clubs: [{ club: "DRIVER", label: "Driver", nominalCarry: 250, normalReach: 270,
        families: [{ family: "FULL", available: true, shapes: [{ shape: "STRAIGHT", available: true }] }] }],
      strategicOptions: [],
    },
  };
  return <main className="command-centre"><style>{"nextjs-portal { display: none !important; }"}</style>
    <PlayMode id="fixture-evidence" event={{ name: "Premium Course Open", location: family, courseType: hole.courseType }}
    situation={situation} hole={hole} scorecard={{ roundNumber: 1, currentHole: hole.holeNumber,
      toPar: 0, totalStrokes: 0, holes: [] }} leaderboard={[]} pressure={null} playerGolferId={null}
    courseGeneratorVersion={8} layoutOverride={params.layout === "before" ? "stacked" : undefined} /></main>;
}
