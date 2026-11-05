import { isValidElement, type ReactElement, type ReactNode } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { Flag } from "lucide-react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { useCareerTraining } from "@/lib/api/career-training";
import type { PlayableSession } from "@/lib/api/career-training";
import { PracticeFacilityNav } from "./practice-facility-nav";
import {
  AutomaticPreparationPreference,
  CareerTraining,
  CareerTrainingConfirmation,
  CareerTrainingPlayableReview,
  CareerTrainingReview,
  WeeklyFocusCard,
  attributeName,
  nextTrainingGain,
  recoveryEffective,
  summarizeImprovements,
} from "./career-training";

vi.mock("@/lib/api/career-training", () => ({
  useCareerTraining: vi.fn(),
  useSetStandingPreparationFocus: () => ({ isPending: false, mutateAsync: vi.fn() }),
  useCommitCareerPreparation: () => ({ isPending: false, mutateAsync: vi.fn() }),
  useStartPlayerTraining: () => ({ isPending: false, mutateAsync: vi.fn() }),
  useAbandonPlayerTraining: () => ({ isPending: false, mutateAsync: vi.fn() }),
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

function elements(node: ReactNode): ReactElement[] {
  if (Array.isArray(node)) return node.flatMap(elements);
  if (!isValidElement(node)) return [];
  return [node, ...elements((node.props as { children?: ReactNode }).children)];
}

function render(override: Record<string, unknown> = {}, worldOverride: Record<string, unknown> = {},
    fitnessOverride: Record<string, unknown> = {}, developmentOptions = options,
    sessionOverride: Record<string, unknown> | null = null) {
  vi.mocked(useCareerTraining).mockReturnValue({
    data: {
      world: { season: 2, week: 7, hasPendingEvent: false, ...worldOverride },
      careerPreparation: { ...preparation, ...override },
      playerFitness: { fatigue: 0.16, availability: "AVAILABLE", injury: null, ...fitnessOverride },
      playerDevelopment: { options: developmentOptions },
      playerProfile: { retired: false, attributes: [{ attribute: "IRONS_ACCURACY", value: 55 }] },
      playerTrainingSession: sessionOverride,
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
    expect(html).toContain("Saved automatic focus");
    expect(html).toContain("Technical development");
    expect(html).toContain("Tournament preparation");
    expect(html).toContain("Recovery");
    expect(html).toContain('aria-valuemax="11"');
    expect(html).toContain("20%");
    expect(html).toContain("Continue with Irons");
    expect(html).toContain("Automatic career preparation");
    expect(html).toContain("No weekly opportunity used until you confirm");
    expect(html).not.toContain("Advance Week");
    expect(html).not.toContain("Advance Season");
  });

  it("uses relevant backend costs and omits the bar at a growth limit", () => {
    expect(nextTrainingGain("IRONS", 2.2, options)).toEqual({ banked: 2.2, threshold: 11, attribute: "IRONS_ACCURACY", percentage: 20 });
    expect(nextTrainingGain("IRONS", 2.2, options.map((item) => ({ ...item, remainingSeasonal: 0 })))).toBeNull();
    expect(nextTrainingGain("TOURNAMENT_PREPARATION", 0, options)).toBeNull();
    expect(nextTrainingGain("IRONS", 2.2, [{ ...options[0], potentialHeadroom: 0 }, options[1]]))
      .toMatchObject({ attribute: "IRONS_CONTROL", threshold: 14 });
  });

  it("uses real attribute names consistently across all technical focuses", () => {
    expect(attributeName("DRIVING_ACCURACY")).toBe("Driving Accuracy");
    expect(attributeName("DRIVING_DISTANCE")).toBe("Driving Distance");
    expect(attributeName("IRONS_ACCURACY")).toBe("Irons Accuracy");
    expect(attributeName("IRONS_CONTROL")).toBe("Irons Control");
    expect(attributeName("WEDGES")).toBe("Wedges");
    expect(attributeName("PUTTING_ACCURACY")).toBe("Putting Accuracy");
    expect(attributeName("PUTTING_PROXIMITY")).toBe("Putting Proximity");
    const cases = [
      ["DRIVING", "DRIVING_DISTANCE", "Driving Distance"],
      ["IRONS", "IRONS_CONTROL", "Irons Control"],
      ["WEDGES", "WEDGES", "Wedges rating"],
      ["PUTTING", "PUTTING_PROXIMITY", "Putting Proximity"],
    ] as const;
    for (const [focus, attribute, label] of cases) {
      const html = render({ standingFocus: focus,
        progress: [{ focus, amount: 2.2 }] }, {}, {},
      [{ attribute, rating: 60, potentialHeadroom: 5, nextCost: 11, remainingSeasonal: 6 }]);
      expect(html).toContain(label);
      expect(html).toContain('aria-valuemax="11"');
    }
  });

  it("makes zero-fatigue Recovery unavailable while retaining it at partial fatigue and injury", () => {
    expect(recoveryEffective(0)).toBe(false);
    expect(recoveryEffective(0.08)).toBe(true);
    expect(recoveryEffective(0.0001)).toBe(true);
    const zero = render({ standingFocus: "RECOVERY" }, {}, { fatigue: 0 });
    expect(zero).toContain("Already fully recovered");
    expect(zero).toContain("Recovery unavailable at 0% fatigue");
    expect(zero).toContain("Choose another activity for this week");
    const partial = render({ standingFocus: "RECOVERY" }, {}, { fatigue: 0.08 });
    expect(partial).toContain("Continue with Recovery");
    expect(partial).not.toContain("Recovery unavailable at 0% fatigue");
    expect(render({ standingFocus: "RECOVERY" }, {}, { fatigue: 0.0001 })).toContain("&lt;0.1%");
    const injury = { type: "WRIST", severity: "MINOR", rehabWeeksRemaining: 2 };
    expect(render({ standingFocus: "RECOVERY" }, {}, { fatigue: 0, availability: "RECOVERING", injury }))
      .toContain("Recovery unavailable at 0% fatigue");
    expect(render({ standingFocus: "RECOVERY" }, {}, { fatigue: 0.08, availability: "RECOVERING", injury }))
      .toContain("Continue with Recovery");
  });

  it("shows the saved standing focus without consuming the available week", () => {
    const html = render({ standingFocus: "PUTTING" });
    expect(html).toContain("Saved automatic focus");
    expect(html).toContain("Putting");
    expect(html).toContain("Available to use");
    expect(html).toContain("Continue with Putting");
  });

  it("keeps the future preference separate and requires an explicit save", () => {
    const onDraftChange = vi.fn();
    const onSave = vi.fn();
    const props = { id: "test-world", pending: false, onDraftChange, onSave };
    const weeklySelection = vi.fn();
    const card = WeeklyFocusCard({
      item: { key: "WEDGES", label: "Wedges", short: "Wedge skill growth", benefit: "Wedge play",
        description: "Build touch and control closer to the green.", icon: Flag },
      selected: false, onSelect: weeklySelection,
    });
    card.props.onClick();
    expect(weeklySelection).toHaveBeenCalledOnce();
    expect(onDraftChange).not.toHaveBeenCalled();
    expect(onSave).not.toHaveBeenCalled();

    const controls = AutomaticPreparationPreference({ ...props, saved: "IRONS", draft: "WEDGES" });
    const select = elements(controls).find((element) => element.type === "select");
    const saveButton = elements(controls).find((element) => typeof (element.props as { onClick?: unknown }).onClick === "function");
    if (!select || !saveButton) throw new Error("Automatic focus controls missing");
    (select.props as { onChange: (event: { target: { value: string } }) => void }).onChange({ target: { value: "PUTTING" } });
    expect(onDraftChange).toHaveBeenCalledWith("PUTTING");
    expect(onSave).not.toHaveBeenCalled();
    (saveButton.props as { onClick: () => void }).onClick();
    expect(onSave).toHaveBeenCalledOnce();

    const unsaved = renderToStaticMarkup(<AutomaticPreparationPreference {...props} saved="IRONS" draft="WEDGES" />);
    expect(unsaved).toContain("Saved focus: <strong");
    expect(unsaved).toContain("Irons</strong>");
    expect(unsaved).toContain('value="WEDGES" selected=""');
    expect(unsaved).toContain("Unsaved selection");
    expect(unsaved).toContain("Save automatic focus");
    expect(unsaved).toContain("Saving affects future eligible weeks only");
    const saved = renderToStaticMarkup(<AutomaticPreparationPreference {...props} saved="WEDGES" draft="WEDGES" />);
    expect(saved).toContain("Saved focus: <strong");
    expect(saved).toContain("Wedges</strong>");
    expect(saved).not.toContain("Save automatic focus");
    expect(saved).toContain("Saved</span>");
  });

  it("prioritises completion and future standing focus when the week is spent", () => {
    const html = render({ available: false, consumedSeason: 2, consumedWeek: 7, committedFocus: "RECOVERY" });
    expect(html).toContain("Preparation complete.");
    expect(html).toContain("Recovery complete");
    expect(html).toContain("Automatic career preparation");
    expect(html).not.toContain("Focus already saved");
    expect(html).not.toContain("Continue with Irons");
    expect(html).not.toContain("This week&#x27;s preparation</h2>");
    expect(html).not.toContain("Review this result");
  });

  it("resumes the persisted active session without offering another weekly commitment", () => {
    const html = render({ available: false, consumedSeason: 2, consumedWeek: 7, committedFocus: "IRONS" },
      {}, {}, options, { id: "week-session", focus: "IRONS", season: 2, week: 7,
        status: "ACTIVE", nextChallenge: 3, challenges: Array.from({ length: 8 }, (_, index) => ({ number: index + 1 })),
        attempts: [{ number: 1, score: 0.4 }, { number: 2, score: 0.7 }] });
    expect(html).toContain("Playable session in progress.");
    expect(html).toContain("2 of 8 attempts recorded");
    expect(html).toContain("Last recorded: Challenge 2 · 70% score.");
    expect(html).toContain("Resume Session");
    expect(html).toContain("Abandon Session");
    expect(html).not.toContain("Continue with Irons");
  });

  it("shows a pending tournament without offering a commit action", () => {
    const html = render({ available: false }, { hasPendingEvent: true });
    expect(html).toContain("Tournament play pending");
    expect(html).toContain("Finish the current event");
    expect(html).toMatch(/<button[^>]*disabled=""[^>]*>Continue with Irons/);
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
  it("offers playable and simulated technical training with distinct rewards", () => {
    const html = renderToStaticMarkup(<CareerTrainingConfirmation focus="IRONS" rules={rules}
      season={2} week={7} pending={false} onBack={() => {}} onCommit={() => {}} onPlay={() => {}} />);
    expect(html).toContain("How would you like to train?");
    expect(html).toContain("Play Session");
    expect(html).toContain("Start Play Session");
    expect(html).toContain("eight real challenges");
    expect(html).toContain("Simulate Session");
    expect(html).toContain("7.5%");
    expect(html).toContain("Season 2 · Week 7");
    expect(html).not.toContain("&amp;apos;");
  });

  it("uses a simple confirmation for recovery", () => {
    const html = renderToStaticMarkup(<CareerTrainingConfirmation focus="RECOVERY" rules={rules}
      season={2} week={7} pending={false} onBack={() => {}} onCommit={() => {}} onPlay={() => {}} />);
    expect(html).toContain("Confirm your preparation.");
    expect(html).toContain("Confirm preparation");
    expect(html).not.toContain("Play Session");
    expect(html).not.toContain("Simulate Session");
  });

  it("celebrates an actual attribute gain and shows the next eligible target", () => {
    const improvements = summarizeImprovements(
      [{ attribute: "IRONS_ACCURACY", delta: 1 }],
      [{ attribute: "IRONS_ACCURACY", value: 74 }],
      [{ attribute: "IRONS_ACCURACY", value: 75 }],
    );
    const html = renderToStaticMarkup(<CareerTrainingReview id="test-world" onOverview={() => {}}
      improvements={improvements}
      progress={{
        before: { banked: 9.97875, threshold: 11, attribute: "IRONS_ACCURACY", percentage: 9.97875 / 11 * 100 },
        after: { banked: 0, threshold: 14, attribute: "IRONS_CONTROL", percentage: 0 },
      }} outcome={{
      committed: true, focus: "IRONS", season: 2, week: 7, baseProgress: 0.95,
      simulatedBonusProgress: 0.07125, progressAwarded: 1.02125,
      attributeImprovements: [{ attribute: "IRONS_ACCURACY", delta: 1, season: 2 }],
      fatigueBefore: 0.16, fatigueAfter: 0.195, fatigueChange: 0.035, readinessPending: false,
    }} />);
    expect(html).toContain("A permanent skill gain.");
    expect(html).toContain("Permanent attribute improvement");
    expect(html).toContain("Irons Accuracy 74 → 75");
    expect(html).toContain("Progress toward");
    expect(html).toContain("Irons Control");
    expect(html).toContain('aria-valuemax="14"');
    expect(html).toContain("Before this session: 91% toward Irons Accuracy");
    expect(html).toContain("16% → 19.5%");
    expect(html).toContain("Used once");
    expect(html).toContain("<details");
    expect(html).toContain("Show result breakdown");
    expect(html).toContain("1.02 focus progress");
    expect(html.indexOf("Progress toward the next attribute gain")).toBeLessThan(html.indexOf("Base progress"));
  });

  it("shows backend-derived before and after progress without an attribute gain", () => {
    const html = renderToStaticMarkup(<CareerTrainingReview id="test-world" onOverview={() => {}}
      improvements={[]}
      progress={{
        before: { banked: 2.2, threshold: 11, attribute: "IRONS_ACCURACY", percentage: 20 },
        after: { banked: 3.22125, threshold: 11, attribute: "IRONS_ACCURACY", percentage: 3.22125 / 11 * 100 },
      }} outcome={{
        committed: true, focus: "IRONS", season: 2, week: 7, baseProgress: 0.95,
        simulatedBonusProgress: 0.07125, progressAwarded: 1.02125, attributeImprovements: [],
        fatigueBefore: 0.16, fatigueAfter: 0.195, fatigueChange: 0.035, readinessPending: false,
      }} />);
    expect(html).toContain("29%");
    expect(html).toContain("Before this session: 20%");
    expect(html).toContain('aria-valuemax="11"');
  });

  it("uses the real Wedges attribute name in the result progress bar", () => {
    const html = renderToStaticMarkup(<CareerTrainingReview id="test-world" onOverview={() => {}}
      improvements={[]} progress={{ before: null,
        after: { banked: 3, threshold: 12, attribute: "WEDGES", percentage: 25 } }} outcome={{
          committed: true, focus: "WEDGES", season: 2, week: 7, baseProgress: 0.95,
          simulatedBonusProgress: 0.07125, progressAwarded: 1.02125, attributeImprovements: [],
          fatigueBefore: 0.16, fatigueAfter: 0.195, fatigueChange: 0.035, readinessPending: false,
        }} />);
    expect(html).toContain("Progress toward <span");
    expect(html).toContain("Wedges rating");
    expect(html).not.toContain("Now toward wedges");
  });

  it("groups genuine multiple gains and falls back to reported deltas when rating reads disagree", () => {
    const changes = [
      { attribute: "DRIVING_ACCURACY", delta: 1 },
      { attribute: "DRIVING_ACCURACY", delta: 1 },
      { attribute: "DRIVING_DISTANCE", delta: 1 },
    ];
    const before = [{ attribute: "DRIVING_ACCURACY", value: 72 }, { attribute: "DRIVING_DISTANCE", value: 70 }];
    const after = [{ attribute: "DRIVING_ACCURACY", value: 74 }, { attribute: "DRIVING_DISTANCE", value: 71 }];
    const improvements = summarizeImprovements(changes, before, after);
    expect(improvements).toEqual([
      { attribute: "DRIVING_ACCURACY", delta: 2, before: 72, after: 74 },
      { attribute: "DRIVING_DISTANCE", delta: 1, before: 70, after: 71 },
    ]);
    const html = renderToStaticMarkup(<CareerTrainingReview id="test-world" onOverview={() => {}}
      improvements={improvements} progress={null} outcome={{
        committed: true, focus: "DRIVING", season: 2, week: 7, baseProgress: 0.95,
        simulatedBonusProgress: 0.07125, progressAwarded: 1.02125,
        attributeImprovements: changes.map((change) => ({ ...change, season: 2 })),
        fatigueBefore: 0.16, fatigueAfter: 0.195, fatigueChange: 0.035, readinessPending: false,
      }} />);
    expect(html).toContain("Driving Accuracy 72 → 74");
    expect(html).toContain("Driving Distance 70 → 71");
    expect(summarizeImprovements(changes, before, [{ attribute: "DRIVING_ACCURACY", value: 73 }, after[1]]))
      .toContainEqual({ attribute: "DRIVING_ACCURACY", delta: 2, before: null, after: null });
  });

  it("shows actual recovery and readiness outcomes", () => {
    const recovery = renderToStaticMarkup(<CareerTrainingReview id="test-world" progress={null} improvements={[]} onOverview={() => {}} outcome={{
      committed: true, focus: "RECOVERY", season: 2, week: 7, baseProgress: 0,
      simulatedBonusProgress: 0, progressAwarded: 0, attributeImprovements: [],
      fatigueBefore: 0.3, fatigueAfter: 0.18, fatigueChange: -0.12, readinessPending: false,
    }} />);
    expect(recovery).toContain("12% ");
    expect(recovery).toContain("fatigue recovered");
    const readiness = renderToStaticMarkup(<CareerTrainingReview id="test-world" progress={null} improvements={[]} onOverview={() => {}} outcome={{
      committed: true, focus: "TOURNAMENT_PREPARATION", season: 2, week: 7, baseProgress: 0,
      simulatedBonusProgress: 0, progressAwarded: 0, attributeImprovements: [],
      fatigueBefore: 0.16, fatigueAfter: 0.16, fatigueChange: 0, readinessPending: true,
    }} />);
    expect(readiness).toContain("Next-tournament readiness");
    expect(readiness).toContain("Ready for the next tournament played");
    expect(readiness).not.toContain("Progress toward the next attribute gain");
  });

  it("does not show a misleading bar at a growth limit or when refresh fails", () => {
    const outcome = {
      committed: true, focus: "WEDGES", season: 2, week: 7, baseProgress: 0.95,
      simulatedBonusProgress: 0.07125, progressAwarded: 1.02125, attributeImprovements: [],
      fatigueBefore: 0.16, fatigueAfter: 0.195, fatigueChange: 0.035, readinessPending: false,
    };
    const limited = renderToStaticMarkup(<CareerTrainingReview id="test-world" progress={{ before: null, after: null }} improvements={[]}
      onOverview={() => {}} outcome={outcome} />);
    expect(limited).toContain("limited by potential or this season");
    expect(limited).not.toContain('role="progressbar"');
    const noRefresh = renderToStaticMarkup(<CareerTrainingReview id="test-world" progress={null} improvements={[]}
      onOverview={() => {}} outcome={outcome} />);
    expect(noRefresh).toContain("could not be refreshed");
    expect(noRefresh).not.toContain('role="progressbar"');
  });

  it("reviews persisted playable score, bonus and genuine rating changes", () => {
    const session = {
      id: "week-session", focus: "IRONS", season: 2, week: 7, status: "COMPLETED",
      attempts: Array.from({ length: 8 }, (_, index) => ({ number: index + 1, score: 0.8 })),
      score: 0.8, baseProgress: 0.95, bonusProgress: 0.114, bonusSettled: true,
      fatigueBefore: 0.16, fatigueAfter: 0.195,
      ratingChanges: [{ attribute: "IRONS_ACCURACY", before: 74, after: 75 }],
      attributeImprovements: [{ attribute: "IRONS_ACCURACY", delta: 1 }],
    } as unknown as PlayableSession;
    const html = renderToStaticMarkup(<CareerTrainingPlayableReview id="test-world" session={session}
      gain={{ banked: 2, threshold: 11, attribute: "IRONS_ACCURACY", percentage: 2 / 11 * 100 }}
      onOverview={() => {}} />);
    expect(html).toContain("Eight challenges complete.");
    expect(html).toContain("Irons Accuracy 74 → 75");
    expect(html).toContain("80%");
    expect(html).toContain("0.11 focus progress");
    expect(html).toContain("16% → 19.5%");
    expect(html).toContain("Used once");
    expect(html).toContain("Progress toward");
  });

  it("shows an abandoned session with retained base progress and no performance bonus", () => {
    const session = {
      id: "abandoned-session", focus: "WEDGES", season: 2, week: 7, status: "ABANDONED",
      attempts: [{ number: 1, score: 0.6 }], score: 0.6, baseProgress: 0.95,
      bonusProgress: 0, bonusSettled: false, fatigueBefore: 0.08, fatigueAfter: 0.115,
      ratingChanges: [], attributeImprovements: [],
    } as unknown as PlayableSession;
    const html = renderToStaticMarkup(<CareerTrainingPlayableReview id="test-world" session={session}
      gain={null} onOverview={() => {}} />);
    expect(html).toContain("Session ended.");
    expect(html).toContain("Base development retained.");
    expect(html).toContain("The performance bonus was forfeited.");
    expect(html).toContain("No performance bonus");
    expect(html).toContain("Used once");
  });
});
