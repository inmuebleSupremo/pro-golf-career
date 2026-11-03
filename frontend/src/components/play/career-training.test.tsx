import { renderToStaticMarkup } from "react-dom/server";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { useCareerTraining } from "@/lib/api/career-training";
import { PracticeFacilityNav } from "./practice-facility-nav";
import {
  CareerTraining,
  CareerTrainingConfirmation,
  CareerTrainingReview,
  nextTrainingGain,
} from "./career-training";

vi.mock("@/lib/api/career-training", () => ({
  useCareerTraining: vi.fn(),
  useSetStandingPreparationFocus: () => ({ isPending: false, mutateAsync: vi.fn() }),
  useCommitCareerPreparation: () => ({ isPending: false, mutateAsync: vi.fn() }),
}));

const rules = {
  trainingBaseProgress: 0.95,
  simulatedBonusProgress: 0.07125,
  totalSimulatedProgress: 1.02125,
  trainingFatigue: 0.035,
  recoveryReduction: 0.12,
  readinessSupport: 0.04,
};

const preparation = {
  standingFocus: "IRONS",
  available: true,
  consumedSeason: 0,
  consumedWeek: 0,
  committedFocus: null,
  ready: false,
  progress: [
    { focus: "DRIVING", amount: 0 }, { focus: "IRONS", amount: 2.2 },
    { focus: "WEDGES", amount: 0 }, { focus: "PUTTING", amount: 0 },
  ],
  rules,
};

const options = [
  { attribute: "IRONS_ACCURACY", rating: 55, potentialHeadroom: 10, nextCost: 11, remainingSeasonal: 6 },
  { attribute: "IRONS_CONTROL", rating: 62, potentialHeadroom: 6, nextCost: 14, remainingSeasonal: 6 },
];

function render(override: Record<string, unknown> = {}, worldOverride: Record<string, unknown> = {}) {
  vi.mocked(useCareerTraining).mockReturnValue({
    data: {
      world: { season: 2, week: 7, hasPendingEvent: false, ...worldOverride },
      careerPreparation: { ...preparation, ...override },
      playerFitness: { fatigue: 0.16, availability: "AVAILABLE", injury: null },
      playerDevelopment: { options },
      playerProfile: { retired: false },
    },
    isLoading: false,
    isError: false,
  } as unknown as ReturnType<typeof useCareerTraining>);
  return renderToStaticMarkup(<CareerTraining id="test-world" />);
}

describe("CareerTraining selection", () => {
  beforeEach(() => vi.clearAllMocks());

  it("shows the compact status, six choices, one primary action and backend-derived skill progress", () => {
    const html = render();
    expect(html).toContain("What would you like your golfer to work on this week?");
    expect(html).toContain("Season 2 · Week 7");
    expect(html).toContain("Available to use");
    expect(html).toContain("Golfer fatigue");
    expect(html).toContain("Automatic focus");
    expect(html).toContain("Technical development");
    expect(html).toContain("Tournament preparation");
    expect(html).toContain("Recovery");
    expect(html).toContain('aria-valuemax="11"');
    expect(html).toContain("20%");
    expect(html).toContain("Continue");
    expect(html).toContain("No weekly opportunity used until you confirm");
    expect(html).not.toContain("Advance Week");
    expect(html).not.toContain("Advance Season");
  });

  it("uses relevant backend costs and omits the bar at a growth limit", () => {
    expect(nextTrainingGain("IRONS", 2.2, options)).toEqual({ banked: 2.2, threshold: 11, percentage: 20 });
    expect(nextTrainingGain("IRONS", 2.2, options.map((item) => ({ ...item, remainingSeasonal: 0 })))).toBeNull();
    expect(nextTrainingGain("TOURNAMENT_PREPARATION", 0, options)).toBeNull();
  });

  it("shows the saved standing focus without consuming the available week", () => {
    const html = render({ standingFocus: "PUTTING" });
    expect(html).toContain("Automatic focus");
    expect(html).toContain("Putting");
    expect(html).toContain("Available to use");
    expect(html).toContain("Continue");
  });

  it("prioritises completion and future standing focus when the week is spent", () => {
    const html = render({ available: false, consumedSeason: 2, consumedWeek: 7, committedFocus: "RECOVERY" });
    expect(html).toContain("Preparation complete.");
    expect(html).toContain("Recovery complete");
    expect(html).toContain("Set a future focus");
    expect(html).toContain("Focus already saved");
    expect(html).toContain("Changing the saved focus is free");
    expect(html).not.toContain(">Continue");
    expect(html).not.toContain("Review this result");
  });

  it("shows a pending tournament without offering a commit action", () => {
    const html = render({ available: false }, { hasPendingEvent: true });
    expect(html).toContain("Tournament play pending");
    expect(html).toContain("Finish the current event");
    expect(html).not.toContain("Save Irons as automatic focus");
    expect(html).toMatch(/<button[^>]*disabled=""[^>]*>Continue/);
  });

  it("groups the existing Free Practice destinations apart from Career Training", () => {
    const html = renderToStaticMarkup(<PracticeFacilityNav id="test-world" active="training" />);
    expect(html).toContain("Free practice");
    expect(html).toContain('/career/test-world/practice"');
    expect(html).toContain('/career/test-world/practice/putting"');
    expect(html).toContain('/career/test-world/practice/training"');
  });
});

describe("CareerTraining confirmation and review", () => {
  it("offers simulated technical training and explains the unavailable playable path", () => {
    const html = renderToStaticMarkup(<CareerTrainingConfirmation focus="IRONS" rules={rules}
      season={2} week={7} pending={false} onBack={() => {}} onCommit={() => {}} />);
    expect(html).toContain("How would you like to train?");
    expect(html).toContain("Play Session");
    expect(html).toContain("Coming in Milestone 3");
    expect(html).toContain("Simulate Session");
    expect(html).toContain("Simulate training");
    expect(html).toContain("Season 2 · Week 7");
    expect(html).not.toContain("&amp;apos;");
  });

  it("uses a simple confirmation for recovery", () => {
    const html = renderToStaticMarkup(<CareerTrainingConfirmation focus="RECOVERY" rules={rules}
      season={2} week={7} pending={false} onBack={() => {}} onCommit={() => {}} />);
    expect(html).toContain("Confirm your preparation.");
    expect(html).toContain("Confirm preparation");
    expect(html).not.toContain("Play Session");
    expect(html).not.toContain("Simulate Session");
  });

  it("shows actual reported development and fatigue while keeping the breakdown optional", () => {
    const html = renderToStaticMarkup(<CareerTrainingReview id="test-world" onOverview={() => {}} outcome={{
      committed: true, focus: "IRONS", season: 2, week: 7, baseProgress: 0.95,
      simulatedBonusProgress: 0.07125, progressAwarded: 1.02125,
      attributeImprovements: [{ attribute: "IRONS_ACCURACY", delta: 1, season: 2 }],
      fatigueBefore: 0.16, fatigueAfter: 0.195, fatigueChange: 0.035, readinessPending: false,
    }} />);
    expect(html).toContain("Good work this week.");
    expect(html).toContain("+1.02");
    expect(html).toContain("irons accuracy +1");
    expect(html).toContain("16% → 19.5%");
    expect(html).toContain("Used once");
    expect(html).toContain("<details");
    expect(html).toContain("Show result breakdown");
  });

  it("shows actual recovery and readiness outcomes", () => {
    const recovery = renderToStaticMarkup(<CareerTrainingReview id="test-world" onOverview={() => {}} outcome={{
      committed: true, focus: "RECOVERY", season: 2, week: 7, baseProgress: 0,
      simulatedBonusProgress: 0, progressAwarded: 0, attributeImprovements: [],
      fatigueBefore: 0.3, fatigueAfter: 0.18, fatigueChange: -0.12, readinessPending: false,
    }} />);
    expect(recovery).toContain("12% ");
    expect(recovery).toContain("fatigue recovered");
    const readiness = renderToStaticMarkup(<CareerTrainingReview id="test-world" onOverview={() => {}} outcome={{
      committed: true, focus: "TOURNAMENT_PREPARATION", season: 2, week: 7, baseProgress: 0,
      simulatedBonusProgress: 0, progressAwarded: 0, attributeImprovements: [],
      fatigueBefore: 0.16, fatigueAfter: 0.16, fatigueChange: 0, readinessPending: true,
    }} />);
    expect(readiness).toContain("Next-tournament readiness");
    expect(readiness).toContain("Ready for the next tournament played");
  });
});
