import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";

import type { TrainingScene, PlayableSession } from "@/lib/api/career-training";
import type { Situation } from "@/components/play/play-shared";
import { BallStrikeDock } from "./ball-strike-dock";
import { TrainingChallengeBriefing } from "./career-training";
import { DrivingRange } from "./driving-range";
import { GreensideTraining } from "./greenside-training";
import { ReadGreenButton } from "./play-mode";
import { PracticePuttingGreen } from "./practice-putting-green";
import { Button } from "@/components/ui/button";

const range = { practiceRange: {
  ballsHit: 0, shotRevision: "0", aimHalfWidth: 220, fieldHalfWidth: 250, targetGreenRadius: 11,
  targets: [{ label: "200 yd", position: { x: -110, y: 167 } }], clubs: ["DRIVER", "SEVEN_IRON"].map((club) => ({
    club, label: club === "DRIVER" ? "Driver" : "7 Iron", nominalCarry: 200, normalReach: 220,
    families: ["FULL", "CONTROLLED"].map((family) => ({ family, available: true, reason: null,
      shapes: [{ shape: "STRAIGHT", available: true, reason: null }] })),
  })),
  expectedReaches: [
    { club: "DRIVER", family: "FULL", yards: 220.4 },
    { club: "DRIVER", family: "CONTROLLED", yards: 180.4 },
    { club: "SEVEN_IRON", family: "FULL", yards: 180.2 },
    { club: "SEVEN_IRON", family: "CONTROLLED", yards: 140.2 },
  ],
} };
const green = { practicePuttingGreen: {
  shotRevision: "0", puttsHit: 0, startingPosition: { x: 0, y: 0 }, readyPosition: { x: 0, y: 0 },
  restingBalls: [], footprint: [{ x: -15, y: -15 }, { x: 15, y: -15 }, { x: 15, y: 15 }, { x: -15, y: 15 }],
  cups: Array.from({ length: 5 }, (_, index) => ({ number: index + 1, position: { x: index - 2, y: 0 } })),
  physicalGreen: { slopeSamples: [], contours: [] },
} };
const greenside = { playerTrainingGreensideHole: {
  holeNumber: 6, par: 4, length: 350, fairwayHalfWidth: 10, greenHalfWidth: 12, greenDepth: 40,
  elevationDelta: 0, hasGreensideBunker: true, hasWater: false, hasTrees: false,
  pinLateral: 0, pinDepth: 0, courseType: "PARKLAND", courseFamily: "PARKLAND", canonicalBiome: "PARKLAND",
  courseGeneratorVersion: 8, canonicalGeometrySource: "GENERATOR_8_PRINCIPLED_ARCHITECTURE",
  layoutSeed: "greenside-training", puttingPolicy: "PHYSICAL_V1",
  effectiveWind: { x: 0, y: 0, magnitude: 0, unit: "EFFECTIVE_YARDS" },
  ball: { position: { x: 0, y: -34 }, lie: "FIRST_CUT" },
  physicalGreen: { slopeSamples: [{ position: { x: 0, y: -4 }, elevation: .2,
    downhillX: .3, downhillY: .1, slope: .02 }], contours: [] },
  geometry: { tee: { x: 0, y: -300 }, cup: { x: 0, y: 0 },
    playableBoundary: [{ x: -70, y: -80 }, { x: 70, y: -80 }, { x: 70, y: 60 }, { x: -70, y: 60 }],
    regions: [
      { surface: "FAIRWAY", boundary: [{ x: -12, y: -78 }, { x: 12, y: -78 }, { x: 12, y: 25 }, { x: -12, y: 25 }] },
      { surface: "FIRST_CUT", boundary: [{ x: -20, y: -78 }, { x: 20, y: -78 }, { x: 20, y: 28 }, { x: -20, y: 28 }] },
      { surface: "FRINGE", boundary: [{ x: -15, y: -28 }, { x: 15, y: -28 }, { x: 15, y: 18 }, { x: -15, y: 18 }] },
      { surface: "GREEN", boundary: [{ x: -12, y: -25 }, { x: 12, y: -25 }, { x: 12, y: 15 }, { x: -12, y: 15 }] },
    ] },
}, playerTrainingGreensideClubs: [{ club: "GAP_WEDGE", label: "Gap Wedge", nominalCarry: 90, normalReach: 90,
  families: ["CHIP", "PITCH"].map((family) => ({ family, available: true, reason: null,
    shapes: [{ shape: "STRAIGHT", available: true, reason: null }] })) }] };

vi.mock("@tanstack/react-query", () => ({
  useQuery: ({ queryKey }: { queryKey: string[] }) => ({
    data: queryKey[0] === "practiceRange" ? range
      : queryKey[0] === "playerTrainingGreensideHole" ? greenside : green, isPending: false, isError: false,
  }),
  useMutation: () => ({ isPending: false, mutateAsync: vi.fn() }),
  useQueryClient: () => ({ setQueryData: vi.fn() }),
}));

function scene(focus: "DRIVING" | "PUTTING", attemptCount = 0): TrainingScene {
  const number = attemptCount + 1;
  const session = { id: "training-session", focus, season: 2, week: 7, status: "ACTIVE",
    attempts: Array.from({ length: attemptCount }, (_, index) => ({ number: index + 1,
      finalPosition: { x: 0, y: 0 } })), nextChallenge: number } as unknown as PlayableSession;
  return { session, challenge: { number, putting: focus === "PUTTING",
    greenside: false,
    start: focus === "PUTTING" ? { x: -3, y: 0 } : { x: 0, y: 0 },
    target: focus === "PUTTING" ? { x: 0, y: 0 } : { x: 85, y: 195 },
    cupNumber: 3, suggestedClub: "DRIVER", suggestedFamily: "CONTROLLED",
    distance: focus === "PUTTING" ? 3 : 212, tolerance: 32, slope: 0.04,
    targetLabel: focus === "PUTTING" ? "Cup 3" : "Zone R-212", landingZone: focus !== "PUTTING" },
    briefingActive: false, challengePlayable: true,
    onReconcile: vi.fn(), onAdvance: vi.fn(), onReview: vi.fn(), onAbandon: vi.fn() } as TrainingScene;
}

describe("playable training in existing practice scenes", () => {
  it("shows a saved legacy range objective without changing its bay or target", () => {
    const html = renderToStaticMarkup(<DrivingRange id="world" training={scene("DRIVING", 2)} />);
    expect(html).toContain("Career Training driving range");
    expect(html).toContain("Challenge 3 of 8");
    expect(html).toContain("Finish at Zone R-212 · 212 yd · Allow for rollout");
    expect(html).toContain("Play challenge");
    expect(html).toContain("Driver · max ~180 yd</option>");
    expect(html).not.toContain("7 Iron · max ~140 yd</option>");
    expect(html).toContain("Approx. maximum carry:");
    expect(html).toContain("Zone R-212 training target");
    expect(html).toContain("Abandon Session");
    expect(html).not.toContain("Normal reach");
    expect(html).not.toContain("To aim");
    expect(html).toContain("Click or tap the range to choose carry and direction");
    expect(html).toContain("Click or tap the range to select an aim point before this challenge");
    expect(html).toMatch(/<button[^>]*disabled=""[^>]*>Play challenge/);
    expect(html).not.toContain('stroke="var(--range-aim)"');
    expect(html).not.toContain("Landing X");
    expect(html).toContain("cursor-crosshair");
    expect(html).toContain("Use arrow keys to adjust direction");
    expect(html).toContain('pointer-events="none"');
  });

  it("highlights an original flag without adding a zone marker or changing the click aim surface", () => {
    const original = scene("DRIVING", 2);
    const training = { ...original, challenge: { ...original.challenge,
      start: { x: 700 / 11, y: 0 }, target: { x: -110, y: 167 }, targetLabel: "200 yd flag", landingZone: false,
      distance: Math.hypot(-110 - 700 / 11, 167) } } as TrainingScene;
    const html = renderToStaticMarkup(<DrivingRange id="world" training={training} />);
    expect(html).toContain("200 yd flag · training target");
    expect(html).toContain('stroke="var(--range-active)" stroke-width="3"');
    expect(html).not.toContain("Zone R-212 training target");
    expect(html).toContain("cursor-crosshair");
    expect(html).toContain("pointer-events=\"none\"");
    expect(html).toContain("Bay 5");
  });

  it("keeps an incompatible saved origin on the safe exit screen without crashing the scene", () => {
    const original = scene("DRIVING");
    const incompatible = { ...original, challenge: { ...original.challenge, start: { x: 999, y: 0 } },
      challengePlayable: false } as TrainingScene;
    expect(renderToStaticMarkup(<DrivingRange id="world" training={incompatible} />))
      .toContain("Career Training driving range");
  });

  it("shows the generated putting start, cup and conditions with placement locked", () => {
    const html = renderToStaticMarkup(<PracticePuttingGreen id="world" training={scene("PUTTING")} />);
    expect(html).toContain("Career Training putting green");
    expect(html).toContain("Challenge 1 of 8");
    expect(html).toContain("Cup 3");
    expect(html).toContain("9 ft");
    expect(html).toContain("Slope 4%");
    expect(html).toContain("Fixed training cup");
    expect(html).not.toContain("Place ball</button>");
    expect(html).not.toContain("Collect balls</button>");
    expect(html.match(/<g[^>]*data-practice-cup="3"[^>]*>/)?.[0]).toContain('pointer-events="none"');
    expect(html).toContain('aria-label="Aim half a degree left"');
    expect(html).toContain('aria-label="Aim half a degree right"');
    expect(html).toContain('aria-label="Exact intended rolling distance in feet"');
  });

  it("mounts a genuine wedge challenge in the existing premium green stage and stable shot dock", () => {
    const original = scene("DRIVING");
    const training = { ...original, session: { ...original.session, focus: "WEDGES" },
      challenge: { ...original.challenge, number: 4, greenside: true,
        start: { x: 0, y: -34 }, target: { x: 0, y: 0 }, suggestedClub: "GAP_WEDGE",
        suggestedFamily: "CHIP", distance: 34, targetLabel: "Hole 6 cup", landingZone: false } } as TrainingScene;
    const html = renderToStaticMarkup(<GreensideTraining id="world" training={training} />);
    expect(html).toContain("Career Training greenside wedge challenge");
    expect(html).toContain("Around the green: click or tap to aim a short-game shot");
    expect(html).toContain("Green reading overlay");
    expect(html).toContain("Read green");
    expect(html).toContain('aria-label="Green reading"');
    expect(html).toContain("md:flex-wrap lg:justify-between");
    expect(html).toContain("col-span-2 row-start-3 justify-self-end");
    expect(html).toContain("Flight shape");
    expect(html).toContain("Abandon Session");
    expect(html).toContain('value="CHIP"');
    expect(html).toContain('value="PITCH"');
    expect(html).toContain("Click or tap the green surroundings to select a landing point");
    expect(html).not.toContain("Driving range with seven target flags");
    expect(html).toContain("practice-facility-page");
  });

  it("briefs real greenside lies and bunkers without range instructions", () => {
    const original = scene("DRIVING").challenge;
    const challenge = { ...original, greenside: true, targetLabel: "Hole 6 cup",
      suggestedClub: "SAND_WEDGE", suggestedFamily: "BUNKER" };
    const html = renderToStaticMarkup(<TrainingChallengeBriefing challenge={challenge} focus="WEDGES"
      playable onBegin={() => {}} onClose={() => {}} onAbandon={() => {}} />);
    expect(html).toContain("real greenside bunker");
    expect(html).toContain("Click or tap the green surroundings");
    expect(html).toContain("final resting distance counts");
    expect(html).not.toContain("Click or tap the range");
  });

  it("keeps the greenside dock footprint and controls stable when Read green changes", () => {
    const hole = greenside.playerTrainingGreensideHole;
    const situation: Situation = { holeNumber: 6, par: 4, shotNumber: 1, strokesThisHole: 0,
      distanceToPin: 34, lie: "FIRST_CUT", pinLateral: 0, minReach: 0, maxReach: 320, shotRevision: "0",
      guidance: { safe: hole.geometry.cup, primary: hole.geometry.cup, aggressive: hole.geometry.cup,
        clubs: greenside.playerTrainingGreensideClubs, strategicOptions: [] } };
    const dock = (reading: boolean) => renderToStaticMarkup(<BallStrikeDock situation={situation}
      origin={hole.ball.position} aimPoint={hole.geometry.cup} onAimPoint={() => {}}
      selection={{ club: "GAP_WEDGE", technique: "CHIP", shape: "STRAIGHT" }}
      onSelection={() => {}} onSubmit={async () => {}} locked={false} submitting={false} responsiveAuxiliary
      auxiliary={<div className="flex flex-nowrap items-center gap-2"><ReadGreenButton active={reading}
        onToggle={() => {}} disabled={false} aroundGreen /><Button variant="secondary" size="sm">
          Abandon Session</Button></div>} />);
    const stable = (markup: string) => markup.replace(/aria-pressed="(?:true|false)"/g, 'aria-pressed="STATE"')
      .replace(/<svg\b[^>]*>[\s\S]*?<\/svg>/g, "<svg/>");
    expect(stable(dock(true))).toBe(stable(dock(false)));
    expect(dock(true)).toContain("md:flex-wrap lg:justify-between");
    expect(dock(true)).toContain("col-span-2 row-start-3 justify-self-end");
    expect(dock(true)).toContain("Flight shape");
    expect(dock(true)).toContain("Play shot");
  });

  it("keeps unlimited Free Practice controls separate", () => {
    const rangeHtml = renderToStaticMarkup(<DrivingRange id="world" />);
    const greenHtml = renderToStaticMarkup(<PracticePuttingGreen id="world" />);
    expect(rangeHtml).toContain("Unlimited balls · Seven targets");
    expect(rangeHtml).toContain("Select hitting bay 7");
    expect(rangeHtml).toContain("7 Iron · max ~180 yd</option>");
    expect(rangeHtml).toContain("Selected aim");
    expect(rangeHtml).toContain("Aim 150 yd");
    expect(rangeHtml).toContain("Click or tap the range to aim");
    expect(rangeHtml).not.toContain("Normal reach");
    expect(greenHtml).toContain("Five cups · Place anywhere · Unlimited balls");
    expect(greenHtml).toContain("Place ball");
    expect(greenHtml).toContain("Collect balls");
    expect(greenHtml.match(/<g[^>]*data-practice-cup="3"[^>]*>/)?.[0]).not.toContain('pointer-events="none"');
    expect(rangeHtml + greenHtml).not.toContain("Abandon Session");
  });

  it("updates the selected club reach when the range technique changes", () => {
    const situation: Situation = { holeNumber: 1, par: 0, shotNumber: 1, strokesThisHole: 0,
      distanceToPin: 200, lie: "TEE_BOX", pinLateral: 0, minReach: 0, maxReach: 320, shotRevision: "0",
      guidance: { safe: { x: 0, y: 200 }, primary: { x: 0, y: 200 }, aggressive: { x: 0, y: 200 },
        clubs: range.practiceRange.clubs, strategicOptions: [] } };
    const props = { situation, origin: { x: 0, y: 0 }, aimPoint: { x: 0, y: 200 },
      onAimPoint: () => {}, onSelection: () => {}, onSubmit: async () => {},
      locked: false, submitting: false, directAim: true,
      expectedReaches: range.practiceRange.expectedReaches };
    const full = renderToStaticMarkup(<BallStrikeDock {...props}
      selection={{ club: "DRIVER", technique: "FULL", shape: "STRAIGHT" }} />);
    const controlled = renderToStaticMarkup(<BallStrikeDock {...props}
      selection={{ club: "DRIVER", technique: "CONTROLLED", shape: "STRAIGHT" }} />);
    const shorterAim = renderToStaticMarkup(<BallStrikeDock {...props} aimPoint={{ x: 0, y: 175 }}
      selection={{ club: "DRIVER", technique: "FULL", shape: "STRAIGHT" }} />);
    expect(full).toContain("Approx. maximum carry: <strong");
    expect(full).toContain("Selected aim 200 yd · Intended carry, club limit applies");
    expect(shorterAim).toContain("Selected aim 175 yd · Intended carry, club limit applies");
    expect(full).toContain("220 yd</strong>");
    expect(controlled).toContain("180 yd</strong>");
    expect(full).toContain("Driver · max ~220 yd</option>");
    expect(controlled).toContain("Driver · max ~180 yd</option>");
    expect(full).not.toContain("Normal reach");
  });

  it("keeps fringe chip and pitch controls without putting in the standalone strike dock", () => {
    const fringe: Situation = { holeNumber: 1, par: 4, shotNumber: 3, strokesThisHole: 2,
      distanceToPin: 7, lie: "FRINGE", pinLateral: 0, minReach: 0, maxReach: 100, shotRevision: "fringe",
      guidance: { safe: { x: 0, y: 7 }, primary: { x: 0, y: 7 }, aggressive: { x: 0, y: 7 },
        strategicOptions: [], clubs: [{ club: "GAP_WEDGE", label: "Gap Wedge", nominalCarry: 90, normalReach: 90,
          families: ["CHIP", "PITCH"].map((family) => ({ family, available: true, reason: null,
            shapes: [{ shape: "STRAIGHT", available: true, reason: null }] })) }] } };
    const props = { situation: fringe, origin: { x: 0, y: 0 }, aimPoint: { x: 0, y: 7 },
      onAimPoint: () => {}, onSelection: () => {}, onSubmit: async () => {}, locked: false, submitting: false,
      selection: { club: "GAP_WEDGE", technique: "CHIP", shape: "STRAIGHT" } };
    const html = renderToStaticMarkup(<BallStrikeDock {...props} allowFringePutt={false} />);
    expect(html).toContain('value="CHIP"');
    expect(html).toContain('value="PITCH"');
    expect(html).not.toContain('value="PUTT"');
  });

  it("briefs each generated objective centrally and offers a safe old-session exit", () => {
    const driving = scene("DRIVING");
    const html = renderToStaticMarkup(<TrainingChallengeBriefing challenge={driving.challenge} focus="DRIVING"
      playable onBegin={() => {}} onClose={() => {}} onAbandon={() => {}} />);
    expect(html).toContain("Challenge 1 of 8");
    expect(html).toContain("212 yd to Zone R-212");
    expect(html).toContain("Suggested Driver");
    expect(html).toContain("Finish on the highlighted zone");
    expect(html).toContain("bounce and rollout");
    expect(html).toContain("Begin Challenge");
    expect(html).toContain("Return to overview");
    expect(html).toContain("Scoring tolerance: 32 yd");
    const flag = { ...driving.challenge, targetLabel: "200 yd flag", landingZone: false };
    expect(renderToStaticMarkup(<TrainingChallengeBriefing challenge={flag} focus="DRIVING"
      playable onBegin={() => {}} onClose={() => {}} onAbandon={() => {}} />))
      .toContain("Finish on the highlighted target green");
    const putting = scene("PUTTING");
    expect(renderToStaticMarkup(<TrainingChallengeBriefing challenge={putting.challenge} focus="PUTTING"
      playable onBegin={() => {}} onClose={() => {}} onAbandon={() => {}} />)).toContain("9 ft to Cup 3");
    const incompatible = renderToStaticMarkup(<TrainingChallengeBriefing challenge={driving.challenge} focus="DRIVING"
      playable={false} onBegin={() => {}} onClose={() => {}} onAbandon={() => {}} />);
    expect(incompatible).toContain("earlier club or target combination");
    expect(incompatible).toContain("base development remains earned");
    expect(incompatible).not.toContain("Begin Challenge");
  });
});
